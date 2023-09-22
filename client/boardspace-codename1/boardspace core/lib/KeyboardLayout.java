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


public class KeyboardLayout 
{	double buttonW = 1.0;		// % of keyboard width
	double buttonH = 2.0;		// % of keyboard width
	
	private String[][] str = null;
	public KeyboardLayout(double bw,double bh,String [][]in)
	{ str = in;
	  buttonW = bw;
	  buttonH = bh;
	}
	public String[][] getKeyMap() 
	{ return str; 
	}


static KeyboardLayout Normal_Lower = new KeyboardLayout(0.062,0.1488,new String[][]
	{{"`","1","2","3","4","5","6","7","8","9","0","-","+","Ndel"},
	{"Ntab","q","w","e","r","t","y","u","i","o","p","[","]","\\"},
	 {"Minus-3/16","Caps","Minus-1/8","a","s","d","f","g","h","j","k","l",";","'","Enter"},
	 {"Shift","z","x","c","v","b","n","m",",",".","/","\u2190","\u2192"  },
	 {"Ctrl",CalculatorButton.id.Nspacebar.name(),CalculatorButton.id.CloseKeyboard.name()}});
static KeyboardLayout Normal_Upper = new KeyboardLayout(0.062,0.1488,new String[][]
	{{"~","!","@","#","$","%","^","&","*","(",")","_","+","Ndel"},
	{"Ntab","Q","W","E","R","T","Y","U","I","O","P","{","}","|"},
	{"Minus-3/16","Caps","Minus-1/8","A","S","D","F","G","H","J","K","L",":","\"","Enter"},
	{"Shift","Z","X","C","V","B","N","M","<",">","?","\u2190","\u2192" },
	{"Ctrl",CalculatorButton.id.Nspacebar.name(),CalculatorButton.id.CloseKeyboard.name()}});

static KeyboardLayout Narrow_Lower =  new KeyboardLayout(0.08,0.1558,new String[][]
	{{"1","2","3","4","5","6","7","8","9","0","Ndel"},		
	 {"Ntab","q","w","e","r","t","y","u","i","o","p"},
	 {"Minus-3/16","Caps","Minus-1/8","a","s","d","f","g","h","j","k","l"}, // 
	 {"Shift","z","x","c","v","b","n","m",",","."},	
	 {"Ctrl",CalculatorButton.id.NSymbol.name(),CalculatorButton.id.NNspacebar.name(),"Enter",CalculatorButton.id.NarrowCloseKeyboard.name()  }});

static KeyboardLayout Narrow_Upper = new KeyboardLayout(0.08,0.1558,new String[][]
	{{"1","2","3","4","5","6","7","8","9","0","Ndel"},				
	 {"Ntab","Q","W","E","R","T","Y","U","I","O","P"},
	 {"Minus-3/16","Caps","Minus-1/8","A","S","D","F","G","H","J","K","L"}, 
	 {"Shift","Z","X","C","V","B","N","M", ",",".",},	
	 {"Ctrl",CalculatorButton.id.NSymbol.name(),CalculatorButton.id.NNspacebar.name(),"Enter",CalculatorButton.id.NarrowCloseKeyboard.name()  }});

static KeyboardLayout Narrow_Symbol =new KeyboardLayout(0.08,0.1558,new String[][]
	{{"~","|",  "+",  "*",   "#",  "$", "{",  "}","Ndel"},
	 { "`",  "=",  "/",  "\\", "<",  ">",  "[",  "]" },
	 {"!",  "@",  "%",  "^",  "&",  "*",  "(",  ")", "Nleft","Nright"},
	 {"-",  "_",  "'",  "`",  ":",  ";",  ",",  "?",  },	    		
	 {CalculatorButton.id.NAlpha.name(),
      CalculatorButton.id.NNspacebar.name(),
      "Enter",
      CalculatorButton.id.NarrowCloseKeyboard.name()},		
	});
public double getButtonW() {
	return buttonW;
}
public double getButtonH() {
	return buttonH;
}


}
