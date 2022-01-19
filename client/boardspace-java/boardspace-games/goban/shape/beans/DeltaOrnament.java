
package goban.shape.beans;

import bridge.Polygon;
import lib.GC;
import lib.Graphics;

import java.awt.*;

import goban.shape.shape.LocationProvider;

public class DeltaOrnament implements OrnamentProtocol
{
		LocationProvider where=null;
		Color mycolor = null;
		boolean isclosed;
		public DeltaOrnament(LocationProvider p,Color c,boolean closed)
		{ 	where=p;
			  mycolor=c;
			  isclosed=closed;
		}
		public LocationProvider Location()
		{
			return(where);	
		}
		public void Draw(Graphics g,GridBoard b)
		{	LocationProvider center = b.Square_Center(where);
		  int w = b.Square_Width();
		  int h = b.Square_Height();
		  Polygon poly = new Polygon();
		  poly.addPoint(center.getX(),center.getY()-h/2-1);
		  poly.addPoint(center.getX()-w/2-1,center.getY()+h/3);
		  poly.addPoint(center.getX()+w/2-1,center.getY()+h/3);
		  GC.setColor(g,mycolor);
		  if(isclosed) { poly.fillPolygon(g); } else { poly.framePolygon(g); }
		}

}