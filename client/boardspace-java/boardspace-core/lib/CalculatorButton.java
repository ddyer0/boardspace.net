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
package lib;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;

/** buttons on the calculator */
public class CalculatorButton {
	private double x;
	private double y;
	Text message = null;
	boolean rightJustify = false;
	private boolean center = true;
	private double w;
	private double h;
	public id value;
	private StockArt button;
	private StockArt buttonLeft;
	private StockArt buttonRight;
	public Color textColor = Color.black;
	boolean visible = true;
	boolean enabled = true;
	
	public enum id implements CellId {
	N0("0",'0'),
	N1("1",'1'),
	N2("2",'2'),
	N3("3",'3'),
	N4("4",'4'),
	N5("5",'5'),
	N6("6",'6'),
	N7("7",'7'),
	N8("8",'8'),
	N9("9",'9'),
	Na("a",'a'),
	Nb("b",'b'),
	Nc("c",'c'),
	Nd("d",'d'),
	Ne("e",'e'),
	Nf("f",'f'),
	Ng("g",'g'),
	Nh("h",'h'),
	Ni("i",'i'),
	Nj("j",'j'),
	Nk("k",'k'),
	Nl("l",'l'),
	Nm("m",'m'),
	Nn("n",'n'),
	No("o",'o'),
	Np("p",'p'),
	Nq("q",'q'),
	Nr("r",'r'),
	Ns("s",'s'),
	Nt("t",'t'),
	Nu("u",'u'),
	Nv("v",'v'),
	Nw("w",'w'),
	Nx("x",'x'),
	Ny("y",'y'),
	Nz("z",'z'),

	NA("A",'A'),
	NB("B",'B'),
	NC("C",'C'),
	ND("D",'D'),
	NE("E",'E'),
	NF("F",'F'),
	NG("G",'G'),
	NH("H",'H'),
	NI("I",'I'),
	NJ("J",'J'),
	NK("K",'K'),
	NL("L",'L'),
	NM("M",'M'),
	NN("N",'N'),
	NO("O",'O'),
	NP("P",'P'),
	NQ("Q",'Q'),
	NR("R",'R'),
	NS("S",'S'),
	NT("T",'T'),
	NU("U",'U'),
	NV("V",'V'),
	NW("W",'W'),
	NX("X",'X'),
	NY("Y",'Y'),
	NZ("Z",'Z'),

	
	Ntilde("~",'~'),
	Nlbrack("[",'['),
	Nrbrack("]",']'),
	Ndel("\u232B",0xff),
	Nbackslash("\\",'\\'),
	Ndquote("\"",'"'),
	Ncolon(":",':'),
	Nsemi(";",';'),
	Ncomma(",",','),
	Nperiod(".",'.'),
	Ncarat("^",'^'),
	Nquestion("?",'?'),
	Nlessthan("<",'<'),
	Ngreatertham(">",'>'),
	Nequals("=",'='),
	Nslash("/",'/'),
	Nminus("-",'-'),
	Nplus("+",'+'),
	Nspace(" ",' '),
	Napostrophe("'",'\''),
	Nexclam("!",'!'),
	Natsign("@",'@'),
	Nhash("#",'#'),
	Ndollar("$",'$'),
	Npercent("%",'%'),
	Nampersand("&",'&'),
	Nstar("*",'*'),
	Nlparen("(",'('),
	Nparen(")",')'),
	Nunderscore("_",'_'),
	Nlbrace("{",'{'),
	Nrbrace("}",'}'),
	Nvbar("|",'|'),
	Nbackquote("`",'`'),
	Ndown("\u2193",0x2193),
	Nup("\u2191",0x2191),
	Nleft("\u2190",0x2190,0.6,0.3,1),
	Nright("\u2192",0x2192),
	NarrowCloseKeyboard("\u25bd",0x25bd,0.25,0.2,1),
	CloseKeyboard("\u25bd",0x25bd,1.13,0.4,1),
	Ncaps("Caps",1000,0,0,2),
	Nenter("Enter",1001,0,0,2),
	Nshift("Shift",1002,0,0,2),
	Ncontrol("Ctrl",1003,0,0,2),
	NSymbol("#*%",1004,0,0,2),
	NAlpha("abc",1005,0,0,2),
	Guess("Guess",1006,0,0,2),
	Nspacebar(" ",' ',0.2,0,8.85),
	NNspacebar(" ",' ',0,0,4),
	Clear("clr",-2,0,0,2),
	Cancel("\u26DD",-3),
	NoQE("No QE",-4,0,0,2),
	Text1("message",-5,0,0,2),
	Display("message",-6,0,0,2),
	Ok("bid",-7,0,0,2),
	Text2("message",-8,0,0,2),
	Ntab("\u21e8",'\t',0,0,1.2),
	Halfspace("halfspace",-1,0,0,0.5),
	Fullspace("fullspace",-1,0,0,1.0),
	Minus14("Minus-1/4",-1,-0.25,0,0),
	Minus316("Minus-3/16",-1,-3.0/16,0,0),
	Minus18("Minus-1/8",-1,-0.125,0,0),
	Plus14("PLus-1/4",-1,0.25,0,0),
	Nunk("???",-9,0,0,2);

	String shortName=name();
	public String shortName() { return(shortName); }
	public int ival = 0;
	double dx = 0;
	double dy = 0;
	double dw = 1;
	id(String n,int v) { shortName=n; ival=v; dx=0; dy=0; dw=shortName.length();  }
	id(String n,int v, double dxx,double dyy, double dww)
	{ shortName = n;
	  ival = v;
	  dx = dxx;
	  dy = dyy;
	  dw = dww;
	}
	public static id find(String name)
		{
		for(id item : values())
			{ if(item.shortName.equals(name) || item.name().equals(name)) { return(item); }}
		G.print("Can't find "+name);
		return(Nunk);
		}
	}
	
	void draw(Graphics gc,exCanvas showOn,HitPoint highlight,Rectangle crect)
	{
		if(visible)
		{
		int left = G.Left(crect);
	    int top = G.Top(crect);
	    int width = G.Width(crect);
	    int height = G.Height(crect);

		int xp = (int)(x*width+left);
		int yp = (int)(y*height+top);
	    int ww = (int)(w*width);
	    int hh = (int)(h*height);
	    HitPoint hit = enabled?highlight:null;
		if(button!=null)
		{	if((buttonRight!=null) && (buttonLeft!=null))
			{	// stretched button like the spacebar
				buttonLeft.drawChip(gc, showOn, hh, xp, yp, null , value,null,1.0,1.0);
				buttonRight.drawChip(gc, showOn, hh, xp + ww-hh, yp, null, value,null,1.0,1.0);
				for(int off=hh/2; off<ww-hh; off+=hh/2)
					{ if(button.drawChip(gc,showOn,hh,xp+off,yp,hit,value,null,1.0,1.0))
						{
						// flash blue when the key is activated, red for ordinary rollover
						hit.spriteColor = hit.down ? Color.blue : Color.red;
						hit.spriteRect = new Rectangle(xp-hh/2,yp-hh/2,ww,hh);
						}
					}
			}
			else if(button.drawChip(gc,showOn,ww,xp,yp,hit,value,null,1.0,1.0))
			{	
				// flash blue when the key is activated, red for ordinary rollover
				hit.spriteColor = hit.down ? Color.blue : Color.red;
				hit.spriteRect = button.getChipRectangle(showOn,ww,xp,yp);
			}
		}
		Font f = showOn.largeBoldFont();
		Font keyFont = FontManager.getFont(f,(int)(FontManager.getFontSize(f)*1.5));
		GC.setFont(gc,keyFont);
		if(rightJustify)
			{
			message.drawRight(gc, xp-ww/2, yp-hh/2,ww,hh,textColor,null);
			}
			else {
			message.draw(gc, center, xp-ww/2,yp-hh/2,ww,hh,textColor, null);
			if(button==null && !"".equals(message.getString()))  
				{ GC.frameRect(gc,textColor,  xp-ww/2, yp-hh/3,ww,2*hh/3); }
			}
		}
	}
	
	public CalculatorButton(id v,String m,double xp,double yp,double ww,double hh)
	{	value = v;
		message = TextChunk.split(m);
		x = xp;
		y = yp;
		w = ww;
		h = hh;
		enabled = false;	// no mouse sensitivity

	}

	public CalculatorButton(id v,StockArt c,double xp,double yp,double ww)
	{	
		value = v;
		message = TextChunk.create(value.shortName());
		x = xp;
		y = yp;
		w = ww;
		button = c;
	}
	// for the space bar
	public CalculatorButton(id v,StockArt cleft,StockArt ccenter,StockArt cright,double xp,double yp,double ww,double hh)
	{	
		value = v;
		message =TextChunk.create("");
		x = xp;
		y = yp;
		w = ww;
		h = hh;
		buttonLeft = cleft;
		buttonRight = cright;
		button = ccenter;
	}
}
