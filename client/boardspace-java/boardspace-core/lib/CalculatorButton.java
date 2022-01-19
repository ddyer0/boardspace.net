package lib;

import java.awt.Color;
import java.awt.Rectangle;

import online.common.exCanvas;

/** buttons on the calculator */
public class CalculatorButton {
	double x;
	double y;
	Text message = null;
	boolean rightJustify = false;
	boolean center = true;
	double w;
	double h;
	boolean reverseVideo = false;
	id value;
	StockArt button;
	StockArt buttonLeft;
	StockArt buttonRight;
	Color textColor = Color.black;
	boolean visible = true;
	boolean enabled = true;
	public enum id implements CellId {
	N0("0",0),
	N1("1",1),
	N2("2",2),
	N3("3",3),
	N4("4",4),
	N5("5",5),
	N6("6",6),
	N7("7",7),
	N8("8",8),
	N9("9",9),
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
	Ntab("Tab",'\t'),
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
	Nleft("\u2190",0x2190),
	Nright("\u2192",0x2192),
	NcloseKeyboard("\u25bd",0x25bd),
	Ncaps("Caps",1000),
	Nenter("Enter",1001),
	Nshift("Shift",1002),
	Ncontrol("Ctrl",1003),
	Nspacebar("Bar",' '),
	Back("del",-1),
	Clear("clr",-2),
	Cancel("X",-3),
	NoQE("No QE",-4),
	Text1("message",-5),
	Display("message",-6),
	Ok("bid",-7),
	Text2("message",-8),
	Nunk("???",-9);

	String shortName=name();
	public String shortName() { return(shortName); }
	int ival = 0;
	id(String n,int v) { shortName=n; ival=v; }
	
	public static id find(String name)
		{
		for(id item : values()) { if(item.shortName.equals(name)) { return(item); }}
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
				buttonLeft.drawChip(gc, showOn, null, value, hh, xp , yp,null,1.0,1.0);
				buttonRight.drawChip(gc, showOn, null, value, hh, xp + ww-hh, yp,null,1.0,1.0);
				for(int off=hh/2; off<ww-hh; off+=hh/2)
					{ if(button.drawChip(gc,showOn,hit,value,hh,xp+off,yp,null,1.0,1.0))
						{
						hit.spriteColor = Color.red;
						hit.spriteRect = new Rectangle(xp-hh/2,yp-hh/2,ww,hh);
						}
					}
			}
			else if(button.drawChip(gc,showOn,hit,value,ww,xp,yp,null,1.0,1.0))
			{	
				hit.spriteColor = Color.red;
				hit.spriteRect = button.getChipRectangle(showOn,ww,xp,yp);
			}
		}
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
