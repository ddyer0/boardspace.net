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

package vnc;

import lib.Image;

/**
 * manages the tiles associated with a bitmap
 */
public class TileManager implements VNCConstants {
    
    public final int MAX_TILE_SIZE = DefaultTileSize;
    private TileInterface tiles[][];
    private boolean tileDirty[][];
    private int numXtile;
    private int numYtile;
    private int tileWidth;		// width of a complete tile
    private int tileHeight;		// height of a complete tile
    private int bitmapWidth;	// width of the parent bitmap
    private int bitmapHeight;	// height of the parent bitmap
    private int[]imageDataCache=null;
    public Object screenConfig = new Object();
    /** Creates a new instance of TileManager */
    public TileManager() {

    }
    
    // sets the size of the parent bitmap, and reinitializes all the data
    // if it changes.
    public void setSize(int sw, int sh) {
    	if((sw!=bitmapWidth) || (sh!=bitmapHeight))
    	{
    	synchronized(screenConfig)
    	{
        bitmapWidth = sw;
        bitmapHeight = sh;
        numXtile = bitmapWidth/MAX_TILE_SIZE;
        numYtile = bitmapHeight/MAX_TILE_SIZE;
        tileWidth = bitmapWidth / numXtile;
        tileHeight = bitmapHeight / numYtile;
        if((bitmapWidth%numXtile)>0) { numXtile++; }
        if((bitmapHeight%numYtile)>0) { numYtile++; }
        tiles = new TileInterface[numXtile][numYtile];
        tileDirty = new boolean[numXtile][numYtile];		// tiles are initially clean (and nonexistent)
        imageDataCache = null;
    	}
    	}
    }
    public void setTileDirty(int x, int y)
    {
    	tileDirty[x][y] = true;
    }
    
    public void clearTileDirty(int x, int y)
    {
    	tileDirty[x][y] = false;
    }
    
    public boolean isTileDirty(int x, int y)
    {
        return tileDirty[x][y];
    }
 
    // return true if anything changed
    public boolean processImage(Image image)
    {
         boolean somechanged = false;
        // this is written in a slightly unnatural way, using G.getRGB() to 
        // avoid inefficiencies in the codename1 image API
        int rgb[] = imageDataCache = image.getRGB(imageDataCache);
        int imw = image.getWidth();
        for (int i=0; i < numXtile; i++) 
        	{
        	int subw = Math.min(tileWidth, bitmapWidth-(i*tileWidth));
            for (int j=0; j < numYtile; j++) 
            {
            		int subh = Math.min(tileHeight, bitmapHeight-j*tileHeight);
            		TileInterface tile = tiles[i][j];
            		boolean newtile = false;
            		if (tile==null)
            			{ tile = tiles[i][j] = new ByteTile(i,j,subw,subh);
            			  newtile=true; 
            			}
				    
		             synchronized (tile) {
		            	 boolean changed = tile.update(rgb,i*tileWidth,j*tileHeight,subw,subh,imw);
		            	 if(changed|newtile) 
		            	 	{ setTileDirty(i,j); somechanged = true; 
		            	 	}
				    }}
           }
         
        return(somechanged);
    }
    
    public TileInterface getTile(int x, int y)
    {
        return tiles[x][y];
    }
    
    public int getNumXTile()
    {
        return numXtile;
    }
    
    public int getNumYTile()
    {
        return numYtile;
    }

	public String getScreenConfig() {
		return(""+bitmapWidth+" "+bitmapHeight+" "+tileWidth+" "+tileHeight);
	}
   
    
}
