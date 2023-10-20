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

import java.awt.Rectangle;

import bridge.Config;




/** pop up calculator */
public class Calculator implements Config
{
	
	private static double whiteScale[]={0.5,0.5,1};
	private static double doubleScale[]={0.5,0.5,1.7};
	static public StockArt KeytopW = StockArt.Make("keytop-wide",doubleScale);
	static public StockArt Keytop = StockArt.Make("keytop",whiteScale);
	static public StockArt Calculator = StockArt.Make("calculator",whiteScale);
	static StockArt[]artwork = {KeytopW,Keytop,Calculator };
	static boolean imagesLoaded = false;
	private int context = -1;
	public void setContext(int n) { context = n; }
	public int getContext() { return(context); }
	static synchronized void loadImages(ImageLoader showOn) 
	{	if(!imagesLoaded)
		{
		imagesLoaded = true;
		StockArt.load_masked_images(showOn, IMAGEPATH,artwork);
		}
	}
    public static CalculatorButton StandardButtons[] = { 
    	new CalculatorButton(CalculatorButton.id.Ndel,Keytop,		0.84,0.4,	0.2),
    	new CalculatorButton(CalculatorButton.id.Clear,Keytop,     0.84,0.53,	0.2),
    	new CalculatorButton(CalculatorButton.id.Cancel,Keytop,	0.84,0.65,		0.2),

    	new CalculatorButton(CalculatorButton.id.N7,Keytop,         0.2,0.4,		0.2),
    	new CalculatorButton(CalculatorButton.id.N8,Keytop,         0.38,0.4,	0.2),
    	new CalculatorButton(CalculatorButton.id.N9,Keytop,         0.56,0.4,	0.2),
    		
    	new CalculatorButton(CalculatorButton.id.N4,Keytop,         0.2,0.53,	0.2),
    	new CalculatorButton(CalculatorButton.id.N5,Keytop,         0.38,0.53,	0.2),
    	new CalculatorButton(CalculatorButton.id.N6,Keytop,         0.56,0.53,	0.2),

    	new CalculatorButton(CalculatorButton.id.N1,Keytop,         0.2,0.65,	0.2),
    	new CalculatorButton(CalculatorButton.id.N2,Keytop,         0.38,0.65,	0.2),
    	new CalculatorButton(CalculatorButton.id.N3,Keytop,         0.56,0.65,	0.2),
    		
    	new CalculatorButton(CalculatorButton.id.N0,Keytop,         0.38,0.78,	0.2),

    	new CalculatorButton(CalculatorButton.id.Text1,"",		0.3, 0.9,   0.45,0.15),
    	new CalculatorButton(CalculatorButton.id.Text2,"",		0.70,0.79,	0.2,0.15),
   		new CalculatorButton(CalculatorButton.id.Ok,KeytopW,       		0.70,0.91,	0.2),
    		 
    };
      
	private exCanvas showOn=null;
	private CalculatorButton buttons[]=null;
	private CalculatorButton display = null;
	public long value = 0;
	long forbiddenValue = -1;
	private String svalue = "";
	public boolean cancel = false;
	public boolean done = false;
	public static String formatDisplay(String n)
	{	int len = n.length();
		if(len<=3) { return(n); }
		else return(formatDisplay(n.substring(0, len-3))
						+","
						+ n.substring(len-3));
	}
	
	public Calculator(exCanvas see,CalculatorButton b[])
	{	loadImages(see.loader());
		showOn = see;
		buttons = b;
		display = new CalculatorButton(CalculatorButton.id.Display,"",0.5,0.18,0.6,0.135);
		display.enabled = false;
		display.rightJustify = true;
	}
	public boolean processButton(HitPoint hp)
	{	
		if(hp.hitCode instanceof CalculatorButton.id)
		{
			CalculatorButton.id bcode = (CalculatorButton.id)hp.hitCode;
			if((bcode.ival>='0') && (bcode.ival<='9'))
				{ svalue = null; value = value*10 + (bcode.ival-'0');
				}
			else {
				switch(bcode)
				{
				case NoQE: 
					value=0;
					svalue = bcode.shortName;
					break;
				case Ndel: 
					svalue = null;
					value = value/10;
					break;
				case Clear: 
					svalue = "";
					value = 0;
					break;
				case Ok:
					cancel = "".equals(svalue);
					done = true;
					break;
				case Cancel:
					cancel = true;
					done = true;
					break;
				default: G.Advise(false,"hit unknown button %s",bcode);
				
				}
			}
			return(true);
		}
		return(false);
	}
	public void setEnabled(CalculatorButton.id button,boolean val)
	{
		for(CalculatorButton b : buttons) { if(b.value==button) { b.enabled = val; }}
	}
	public void setVisible(CalculatorButton.id button,boolean val)
	{
		for(CalculatorButton b : buttons) { if(b.value==button) { b.visible = val; }}
	}
	public void setMessage(CalculatorButton.id button,String ms)
	{
		for(CalculatorButton b : buttons) { if(b.value==button) { b.message = TextChunk.split(ms); }}	
	}
	public void draw(Graphics gc,Rectangle crect,HitPoint highlight)
	{	loadImages(showOn.loader());
	  	Calculator.drawChipH(gc, showOn, crect, null);
    	GC.setFont(gc, showOn.largeBoldFont());
    	for(CalculatorButton button : buttons)
    	{	button.draw(gc,showOn,highlight,crect);
    	}
    	display.message = TextChunk.create(svalue==null 
    						? formatDisplay(""+value)
    						: (value==0) ? svalue : formatDisplay(svalue));
    	display.draw(gc, showOn, null, crect);
	}
}
