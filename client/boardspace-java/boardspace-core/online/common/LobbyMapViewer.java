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
package online.common;

import java.awt.*;
import java.util.*;

import bridge.*;
import lib.Image;
import lib.LFrameProtocol;
import lib.Graphics;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
/**
 * this is a chopped down and simplified version of the general map viewer.  Created 8/2020 when
 * a random change uncovered the fact that "MapViewer" for the lobby was accidentally a whole
 * new process and window hierarchy, left over from the standalone map viewer.
 * 
 * @author Ddyer
 *
 */
public class LobbyMapViewer implements LobbyConstants, Config
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;

    static final int latscale = 100000;
    Image map = null;
    int[] playerLocations = null;
    String[] playerNames = null;
    int dataSize = 0;
    private int minsize = 2;
    boolean dataReady = false;
    double centerx = 0.0;
    double centery = 0.0;
    boolean drawn = false;

    public LobbyMapViewer()
    {	
    }

    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
        map = Image.getImage(SMALL_MAP_LOC_URL);
        centerx = G.getDouble(SMALL_MAP_CENTER_X,SMALL_MAP_X_CENTER);
        centery = G.getDouble(SMALL_MAP_CENTER_Y,SMALL_MAP_Y_CENTER);

    }

    public boolean MapDraw(Graphics g, int dx, int dy, int w, int h)
    {

        if (map != null)
        {	double SCALE = G.getDisplayScale();
            int cx = (int) (centerx * w);
            int cy = (int) (centery * h);

            //System.out.println("paint "+w+"x"+h);
            int iw = map.getWidth();
            int ih = map.getHeight();
            if ((iw > 0) && (ih > 0))
            {   drawn = map.drawImage(g,dx, dy, w, h);
                if (dataReady && (dataSize > 0))
                {
                    Hashtable<Integer,Integer> mapped = new Hashtable<Integer,Integer>(dataSize);

                    //the player data is ready. 
                    //latitude
                    for (int i = 0; i < dataSize; i++)
                    {
                        int lat100 = (playerLocations[i] / latscale); //recover hundredths of a degree
                        int lon100 = (playerLocations[i] % latscale); //recover hundredths of a degree
                        int latloc = (int) (lat100 * (h / 18000.0)); //lat loc on the map
                        int lonloc = (int) (lon100 * (w / 36000.0)); //longitude loc on the map
                        Integer key = Integer.valueOf((latloc * latscale) + lonloc); //unique location on the map
                        Integer old = mapped.get(key);
                        int oldval = (old == null) ? 0 : old.intValue();
                        mapped.put(key, Integer.valueOf(1 + oldval));
                    }

                    //ready to display
                    GC.setColor(g,Color.yellow);

                    for (Enumeration<Integer> e = mapped.keys(); e.hasMoreElements();)
                    {
                        Integer key = e.nextElement();
                        Integer nofkey = mapped.get(key);
                        int keyval = key.intValue();
                        int keyn = nofkey.intValue();
                        int y = ((keyval / latscale) + cy) % h;
                        int x = ((keyval % latscale) + cx) % w;

                        //System.out.println("x "+x+" y "+y+" n=" + keyn);
                        int rad = (int) (Math.sqrt(keyn) + 0.99);
                        GC.frameOval(g,dx + x, dy + y,(int)( SCALE*(rad + minsize)), (int)(SCALE*(rad + minsize)));
                    }
                    /*
                    if (nodata >= 0)
                    {
                        int subtotal = dataSize;
                        int total = nodata + subtotal;
                        int pc = (int) (0.5 + ((subtotal * 100.0) / total));
                        G.setColor(g,Color.yellow);
                        G.Text(g,"Data for " + subtotal + " of " + total +
                                " players (" + pc + "%)", 10, h - 10);
                    }
                    */
                }
            }
        }

        return (drawn);
    }


    public void initData(int size)
    {
        dataReady = false;
        dataSize = 0;
        playerLocations = new int[size];
        playerNames = new String[size];
    }

    public void addPlayer(String slat, String slon, String name)
    {
        try
        {	if((slat!=null)&&(slon!=null)&&(!("".equals(slat)))&&(!("".equals(slon))))
        	{
            double rawlat = G.DoubleToken(slat);
            double rawlon = G.DoubleToken(slon);

            //convert latitude from -90 to 90 TO 0-180
            int lat = (int) (90 - rawlat) * 100;

            //convert logitude form -180 to 180 TO 0-360
            int lon = (int) ((rawlon < 0) ? (360.0 + rawlon) : rawlon) * 100;
            playerLocations[dataSize] = (lat * latscale) + lon;
            playerNames[dataSize] = name;
            dataSize++;
        	}
        }
        catch (NumberFormatException e)
        {
        }
    }

    public void setDataReady()
    {
        dataReady = true;
    }

}
