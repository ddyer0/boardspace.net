
package goban.shape.beans;

import lib.Graphics;
import lib.GC;

import java.awt.*;

import goban.shape.shape.LocationProvider;

public class LetterOrnament implements OrnamentProtocol
{
		LocationProvider where=null;
		public String text="";
		
		public LetterOrnament(LocationProvider p,String s)
		{ 	where=p;
				text=s;
		}
		public LocationProvider Location()
		{
			return(where);	
		}
		public void Draw(Graphics g,GridBoard b)
		{	LocationProvider center = b.Square_Center(where);
		  int w = b.Square_Width();
		  int h = b.Square_Height();
			GC.setColor(g,Color.gray);
			GC.fillRect(g,center.getX()-w/4,center.getY()-h/4,w/2,h/2);
			GC.setColor(g,Color.black);
			GC.Text(g,text,center.getX()-w/8,center.getY()+h/8);
		}
}