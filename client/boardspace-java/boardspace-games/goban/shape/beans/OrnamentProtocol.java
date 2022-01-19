
package goban.shape.beans;

import lib.Graphics;

import goban.shape.shape.LocationProvider;

public interface OrnamentProtocol
{
		public LocationProvider Location();
		public void Draw(Graphics g,GridBoard b);
}
