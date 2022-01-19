package online.common;

import java.awt.*;
import java.util.*;

import bridge.*;
import lib.Image;
import lib.LFrameProtocol;
import lib.Utf8Reader;
import lib.Graphics;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.Http;

/** this class was used as part of an applet, but is no longer used in the main app
 * 7/2/2021
 * @author Ddyer
 *
 */
public class MapViewer extends commonPanel implements OnlineConstants, Runnable, LobbyConstants, Config
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;

public synchronized boolean sendMessage(String msg) 
		{ if(myNetConn!=null)  { throw G.Error("shouldn't be called");}
		  return(false); 
		}
    static final int latscale = 100000;
    String server = null;
    Image map = null;
    String imageLocation = null;
    int[] playerLocations = null;
    String[] playerNames = null;
    int dataSize = 0;
    public int minsize = 0;
    boolean dataReady = false;
    double centerx = 0.0;
    double centery = 0.0;
    boolean drawn = false;
    int nodata = -1;

    public MapViewer()
    {	
    }

    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	super.init(info, frame);
        sharedInfo = info;
        server = sharedInfo.getString(Config.SERVERNAME);
        imageLocation = G.getString(IMAGELOCATION, null);
        if(imageLocation==null)
        {
        	map = Image.getImage(SMALL_MAP_LOC_URL);
        }
        else 
        {  map = Image.getURLImage(G.getUrl(imageLocation));
           if(map!=null)
           {
        	   MediaTracker tr = new MediaTracker(this);
        	   tr.addImage(map.getImage(),0);
        	   try { tr.waitForAll(); } catch (InterruptedException ee) {};
           }
        }
        
        centerx = G.getDouble(SMALL_MAP_CENTER_X,SMALL_MAP_X_CENTER);
        centery = G.getDouble(SMALL_MAP_CENTER_Y,SMALL_MAP_Y_CENTER);
    }

    public void update(Graphics G)
    {
        paint(G);
    }

    public boolean MapDraw(Graphics g, int dx, int dy, int w, int h)
    {
        if (map == null)
        {
            map = Image.getImage(imageLocation);
        }

        if (map != null)
        {	double SCALE = G.getDisplayScale();
            int cx = (int) (centerx * w);
            int cy = (int) (centery * h);

            //System.out.println("paint "+w+"x"+h);
            int iw = map.getWidth();
            int ih = map.getHeight();
            if ((iw > 0) && (ih > 0))
            {   drawn =map.drawImage(g, dx, dy, w, h);
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

                    if (nodata >= 0)
                    {
                        int subtotal = dataSize;
                        int total = nodata + subtotal;
                        int pc = (int) (0.5 + ((subtotal * 100.0) / total));
                        GC.setColor(g,Color.yellow);
                        GC.Text(g,"Data for " + subtotal + " of " + total +
                                " players (" + pc + "%)", 10, h - 10);
                    }
                }
            }
        }

        return (drawn);
    }

    public void paint(Graphics g)
    {
        int w = getWidth();
        int h = getHeight();
        MapDraw(g, 0, 0, w, h);
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

    public void run()
    { //fetch the data
     	repaint();
     	getData();
     	super.run();
    }
    private void getData()
    {
        //System.out.println("running");
    	for(int socket : web_server_sockets)
    	{
        try
        {
            Utf8Reader rs = Http.getURLReader(server, socket, Config.mapDataURL);

            if (rs != null)
            {
                String num;

                while ((num = rs.readLine()) != null)
                {
                    //System.out.println("got "+num);
                    StringTokenizer tok = new StringTokenizer(num);

                    {
                        String cmd = tok.nextToken();

                        if ("nodata:".equals(cmd))
                        {
                            nodata = G.IntToken(tok.nextToken());
                        }
                        else if ("rows:".equals(cmd))
                        {
                            int nRows = G.IntToken(tok.nextToken());
                            initData(nRows);

                            while (nRows-- > 0)
                            {
                                String l = rs.readLine();

                                if (l != null)
                                {
                                    StringTokenizer st = new StringTokenizer(l);
                                    String lat = st.nextToken();
                                    String lon = st.nextToken();
                                    String who = st.nextToken();
                                    addPlayer(lat, lon, who);
                                }
                            }

                            setDataReady();
                            repaint();
                        }
                    }
                }

                rs.close();
                return;
            }
        }
    	catch (ThreadDeath err) { throw err;}
        catch (Throwable e)
        {
            Http.postError(this, "map viewer main", e);
        }
    }
    }
}
