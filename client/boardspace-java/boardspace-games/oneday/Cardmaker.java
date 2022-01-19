package oneday;
import java.awt.Color;
import java.awt.Font;
import lib.Graphics;
import lib.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import online.common.exCanvas;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.MouseState;
import lib.StockArt;
import lib.XImage;


public class Cardmaker extends exCanvas implements Runnable
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	Color lightGray = new Color(0.8f,0.8f,0.8f);
	static boolean NOTE_ADJACENT = false;
	static String cardLogo = "http://boardspace.net/english/about_oneday.html";
	
	// numbers appropriate for gamecrafter micro cards, (1.25x1.75 inch) which also need large margins for trim.
	//static int CARD_WIDTH = 450;
	//static int CARD_HEIGHT = 600;
	//static int CARD_MARGIN_PERCENT=10;
	//static int TEXTHEIGHT = 30;
	//Font cardFont = largeBoldFont=new Font("sansserif", Font.BOLD, 17);
	//Font smallCardFont = new Font("sansserif", Font.BOLD, 12);
	//static int CARD_MARGIN_PERCENT=10;

	// numbers appropriate for gamecrafter mini cards, (1.75x2.5 inch)
	//static int CARD_WIDTH = 600;
	//static int CARD_HEIGHT = 825;		// mini size
	
	// numbers appropriate for gamecrafter domino cards, (1.75x3.5 inch)
	static int CARD_WIDTH = 600;
	static int CARD_HEIGHT = 1125;		// domino size (1.75 x 3.5 inch)
	
	static int TEXTHEIGHT = 50;
	static int CARD_MARGIN_PERCENT=12;
	Font titleFont = G.getFont("sansserif",G.Style.Bold,40);
	Font cardFont = G.getFont(titleFont, G.Style.Bold, 30);
	Font smallCardFont = G.getFont(titleFont, G.Style.Bold, 25);
	public Cardmaker()
	{
	}
	static boolean USE_MAP_IMAGES = true;
	static String Map_Image = "london-tube-small-no-stations.png";
	static String Dot_Image[] = {"dot"};
	static double Dot_Scale[] = {0.5,0.5,1.0};
	static StockArt dot = null;
	boolean exit = false;
	public void setExit(boolean v) { exit = v; }
	  String in;
	  String out;
	  Rectangle fullRect = null;
	  StopStack stops = new StopStack();


	  // this needs to exist because the standard getImage depends on theRoot
	  public Image getImage(String url) 
    	{	String name = "file:"+url;
    		try {
    			URL clas = new URL(name);
    			return(Image.createImage(Toolkit.getDefaultToolkit().getImage(clas),name));
    		}
    		catch (MalformedURLException e) { return(null); }
    		
    	}
	  public void drawCard(Graphics g,Rectangle r,Station card,String cardId)
	  {	  int smargin = (r.width*CARD_MARGIN_PERCENT)/100;
	  	  int tmargin = smargin+smargin/2;
	  	  int im_w = card.image.getWidth();
	  	  int im_h = card.image.getHeight();
		  int w = r.width-smargin*2;
		  int h = USE_MAP_IMAGES ? w*im_h/im_w : w;
	  	  GC.fillRect(g,Color.white,r);
	  	  GC.frameRect(g,Color.black,r);
	  	  int tbot = TEXTHEIGHT;
	  	  Rectangle title = new Rectangle(r.x+smargin,r.y+tmargin,w,tbot);
	  	  Rectangle subhead = new Rectangle(r.x+smargin,r.y+tmargin+smargin,w,tbot);
	  	  int image_y = r.y+tmargin+smargin+tbot;
	  	  int image_x = r.x+smargin;
	  	  
	  	  
	  	  // draw the main image for the card
	  	  if(USE_MAP_IMAGES)
	  	  {	Rectangle rect = new Rectangle(image_x,image_y,w,h);
		  	  drawImage(g,card.image,image_x,image_y,w,h);
	  		  card.drawLines(g,rect,1.0);
	  		  dot.drawChip(g,this,TEXTHEIGHT/2,
	  				  (int)(image_x+card.xpos*w/100),
	  				  (int)(image_y+h*card.ypos/100),
	  				  null);
 		  
	  	  }else
	  	  {
	  		card.drawChip(g,this,w,image_x+w/2+smargin,image_y+h/2,null);
	  	  }
	  	  GC.setFont(g,titleFont);
	  	  GC.Text(g,true,title,Color.black,null,card.station);
	  	  GC.setFont(g,cardFont);
	  	  GC.Text(g,true,subhead,Color.black,null,"Pronounced "+card.pronounciation+", " + card.description);
		  int picBottom = image_y+h;
		  boolean allConcurrent = true;
		  Stop[]stops = card.stops;
		  for(int idx=0;idx<stops.length;idx++)
		  {   Stop prev = (idx>0) ? stops[idx-1] : null;
			  Stop st = stops[idx];
			  Line l = st.line;
			  Rectangle stopRect = new Rectangle(r.x+smargin+tbot,picBottom,w-tbot*2,tbot);
			  Rectangle stopRectRight = new Rectangle(r.x+smargin+w-tbot,picBottom,tbot,tbot);
			  Rectangle stopRectLeft = new Rectangle(r.x+smargin,picBottom,tbot,tbot);
			  picBottom += tbot;
			  GC.setFont(g,cardFont);
			  GC.Text(g,true,stopRect,l.textColor,l.color,l.name);
			  // 
			  // generate the + symbols for lines which run concurrently.
			  // this depends on the lines that are concurrent being next
			  // to each other
			  //
			  if(prev!=null)
			  {	Station prevNext = prev.nextStop();
			    Station prevPrev = prev.prevStop();
			    Station n = st.nextStop();
			    Station p = st.prevStop();
				  if( ((prevNext==n) || (prevNext==p) ) && ((prevPrev==n)||(prevPrev==p)))
				  {	  stopRect.y -= stopRect.height/2; 
					  GC.Text(g,true,stopRect,l.textColor,null,"+");
				  }
				  else { allConcurrent = false; }
			  }
			  //
			  // draw the station number at the right
			  //
			  GC.setFont(g,smallCardFont);
			  GC.Text(g,true,stopRectLeft,l.textColor,l.color,""+(st.ordinal-1));
			  GC.Text(g,true,stopRectRight,l.textColor,l.color,""+(st.line.nStops()-st.ordinal));
		  }
		  //
		  // if this is a non-intersection station, note the previous and next stations
		  //
		  if(NOTE_ADJACENT && (stops.length==1 || allConcurrent))
		  {	  Stop st = stops[stops.length-1];
		  	  Rectangle stopRect = new Rectangle(r.x+smargin,picBottom-tbot,w,tbot);
	  	      picBottom += tbot;
			  Station prevStation = st.prevStop();
			  Station nextStation = st.nextStop();
			  stopRect.y += stopRect.height;
			  String msg = ((prevStation==null) ? "" : prevStation.station) + "  --  "+ ((nextStation==null)?"":nextStation.station);
			  GC.Text(g,true,stopRect,Color.black,lightGray,msg);
		  }
		  // add the activity paragraph
		  {
			  Rectangle text = new Rectangle(r.x+smargin,picBottom,w,r.height-tmargin-picBottom);
			  String msg = card.activity;
			  //if(msg.contains("..")) { G.print("cart "+card+" "+msg); }
			  if(msg!=null)
			  	{  String msg2 = msg.replace("\\n","\n");
			  	   GC.Text(g,false,text,Color.black,null,s.lineSplit(msg2,GC.getFontMetrics(g),text.width)); 
			  	}
		  }
		  if(cardId!=null)
		  {	Rectangle bot = new Rectangle(r.x+smargin,r.y+r.height-tmargin-tbot*4/5,w,tbot/3);
		    Rectangle ll = new Rectangle(r.x+smargin,r.y+r.height-tmargin-tbot/2+tbot/5,w,tbot/2);
			GC.Text(g,false,bot,Color.black,null,cardId);
			GC.Text(g,true,ll,Color.black,null,cardLogo);
		  }
	  }
	  Station currentCard = null;
	  public void drawBoardElements(Graphics g,HitPoint pt)
	  {
		  if(currentCard!=null)
		  {	 int inset = 5;
		  	 int w = fullRect.width-2*inset;
		  	 int h = fullRect.height-2*inset;
		  	 int dim = Math.min(w,(h*CARD_WIDTH/CARD_HEIGHT));
		  	 Rectangle r = new Rectangle(inset,inset,dim,(dim*CARD_HEIGHT/CARD_WIDTH));
		  	 drawCard(g,r,currentCard,"first card");
		  }
		  
	  }
      public void drawCanvas(Graphics g, boolean complete,HitPoint pt)
      { // this is the method that really refreshes, will be superseded
        // by the superclass
    	  if((fullRect!=null) && (g!=null))
    	  {	if(currentCard==null)
    	  	{
    		  GC.setColor(g,Color.black);
    		  GC.frameRect(g,Color.black,fullRect);
    		  GC.drawLine(g,0,0,fullRect.width,fullRect.height);
    	  	}
    	  else { drawBoardElements(g,pt); }
    	  }
      }
	  public Connection  connect(String host,String database,String user,String pass) throws SQLException
	  {	String url = "jdbc:mysql://"+host+"/"+database;
	  	return(DriverManager.getConnection(url, user, pass));	  
	  }
	  String lineColorName(String name)
	  {
		  StringTokenizer n = new StringTokenizer(name);
		  String firstName = n.nextToken().toLowerCase();
		  return("Station."+firstName+"Index");
	  }
	  void loadLines(Connection conn,OutputStreamWriter osw) throws SQLException,IOException
	  {
		  String query = "select name,descstr,color,included,runtime,startinterval,startoffset from linenames ";
		  PreparedStatement ret = conn.prepareStatement(query);
		  if(ret.execute())
		  {	ResultSet result = ret.getResultSet();
		  while (result.next())
		  {
			String name = result.getString(1);
			String comm = result.getString(2);
			String rgb = result.getString(3);
			boolean inc = result.getInt(4)==1;
			int run = result.getInt(5);
			int interval = result.getInt(6);
			int startoffset = result.getInt(7);
			StringTokenizer tok = new StringTokenizer(rgb);
			int r = G.IntToken(tok);
			int g = G.IntToken(tok);
			int b = G.IntToken(tok);
			// if black enough we get white lettering. 
			String lineColor = lineColorName(comm);
			int tc = (r+g+b)/3 <100 ? 255 : 0;
			if(osw!=null)
			{
				osw.write("new Line(\"" + name + "\",\""+comm+"\","+r+","+g+","+b+","+tc+","+inc+","+run+","+interval
						+","+startoffset
						+","+lineColor
						+");\n");
			}
			new Line(name,comm,new Color(r,g,b),new Color(tc,tc,tc),inc,run,interval,startoffset,0);
		  }}
	  }
	  void loadStops(Connection conn,OutputStreamWriter osw) throws SQLException,IOException
	  {
		  String query = "select station,line,number from stops left join linenames on stops.line=linenames.name where linenames.included=1";
		  PreparedStatement ret = conn.prepareStatement(query);
		  if(ret.execute())
		  {	ResultSet result = ret.getResultSet();
		  	while (result.next())
		  	{
			String station = result.getString(1);
			String line = result.getString(2);
			String number = result.getString(3);
			if(osw!=null)
			{
			osw.write("new Stop(\""+station+"\",\""+line+"\","+number+");\n");
			}
			new Stop(Station.getStation(station),Line.getLine(line),G.IntToken(number));
		  	}
		  }
	  }
	  void loadTransitions(Connection conn,OutputStreamWriter osw) throws SQLException,IOException
	  {
		  String query = "select station,fromline,toline,time from interchange order by station, fromline, toline";
		  PreparedStatement ret = conn.prepareStatement(query);
		  if(ret.execute())
		  {	ResultSet result = ret.getResultSet();
		  	while (result.next())
		  	{
			String station = result.getString(1);
			String from = result.getString(2);
			String to = result.getString(3);
			String time = result.getString(4);
			if(osw!=null)
			{
			osw.write("new Interchange(\""+station	+"\",\""
					+ from+"\",\""
					+ to+"\",\""
					+ time+"\");\n");
			}
		  	}
		  }
	  }
	  void loadStations(Connection conn,OutputStreamWriter osw) throws SQLException,IOException
	  {
		  String query = "select station,photo,line,loc_x,loc_y,description,pronounciation,activity from stations";
		  Image map_image = USE_MAP_IMAGES ? loader.load_image(in,Map_Image) : null;
	  		PreparedStatement ret = conn.prepareStatement(query);
		    if(ret.execute())
		    {
		    	ResultSet result = ret.getResultSet();
		    	while (result.next())
		    	{	String station = result.getString(1);
		    		String pic = result.getString(2);
		    		String lines = result.getString(3);
		    		double x = G.DoubleToken(result.getString(4));
		    		double y = G.DoubleToken(result.getString(5));
		    		String description = result.getString(6);
		    		String pronounciation = result.getString(7);
		    		String activity = result.getString(8);

		    		Image im = USE_MAP_IMAGES ? map_image : loader.load_image(in,pic);
		    		if(osw!=null)
		    		{
		    			osw.write("new Station(\""+station+"\",\""
		    						+lines+"\",\""
		    						+pic+"\","
		    						+x+","
		    						+y+"," 
		    						+"\""+description+"\","
		    						+"\""+pronounciation+"\","
		    						+"\""+activity
		    						+"\");\n");
		    		}
		    		new Station(station,lines,pic,im,x,y,description,pronounciation,activity);
		    	};
		    	
		    } 
	  }
	  // make a complete set of cards, including duplicates.
	  public void makeResultCards(String out)
	  {
		  int w = CARD_WIDTH;
		  int h = CARD_HEIGHT;
	      XImage allFixed = painter.allFixed();
		  for(int lim = Station.nStations()-1; lim>=0; lim--)
		  {	Rectangle r = new Rectangle(0,0,w,h);
			  Station st = Station.getStation(lim);
			  Graphics g = allFixed.getGraphics();
			  for(int copy = 1,ncopies=st.nCopies(); copy<=ncopies; copy++)
				  { String text = (lim==1)?""+(lim+1):""+(lim+1)+"-"+copy;
				  	currentCard = st;
					drawCard(g,r,st,text);
					  try
					  {
					  File outf = new File(out+st.station+"-"+text+".png");
					  ImageIO.write((BufferedImage)allFixed.getImage().getImage(), "png",outf);
					  }
					  catch (IOException e)
					  {
						  G.print("Error "+e.toString());
					  }}
		  }
	  }
	  public void makeCards(String host,String database,String user,String pass,String ins,String outs,String classStr)
			  	throws SQLException, IOException
	  {	Connection conn = connect(host,database,user,pass);
	  	in = ins;
	  	out = outs;
	  	if(conn!=null)
	  	{	Image im[] = loader.load_masked_images(ins,Dot_Image);
	  		dot = new StockArt(Dot_Image[0],Dot_Scale,im[0]);
	  		File classFile = new File(classStr);
	  		if(classFile!=null)
	  		{
	  		FileOutputStream outFile = new FileOutputStream(classFile);
	  		try {
	  		OutputStreamWriter osw = null; 
	  		if(outFile!=null)
	  		{
	  		String className = classFile.getName();
	  		int dot = className.lastIndexOf('.');
	  		if(dot>=0) { className = className.substring(0,dot); }
	  		osw = new OutputStreamWriter(outFile);   
	  		osw.write("package oneday;\n");
	  		osw.write("\n// do not edit, derived from the London database.  run the  Cardmaker.main\n\n");
	  		osw.write("public class "+className +" {\n");
	  		osw.write("public static void loadData()\n{\n");
	  		}
	  		loadLines(conn,osw);
	  		loadStations(conn,osw);
	  		loadStops(conn,osw);
	  		loadTransitions(conn,osw);
	  		if(osw!=null)
	  		{
	  			osw.write(" }}\n");
	  			osw.close();
	  		}
 
	  		Station.findAllStops();
	  		currentCard = Station.getStation(5);
	  		makeResultCards(outs);
	  		} 
	  		finally 
	  		{
	  		if(outFile!=null) { outFile.close(); }
	  		}}
	  	}
	  }
	  public static void main(String args[])
	  {	  String pass = "";
	  	  String host = "";
	  	  String database = "";
	  	  String user = "";
	  	  String out = null;
	  	  String in = null;
	  	  String classFile = null;
	  	  boolean trouble = (args.length==0) || ((args.length&1)!=0);
	  	  for(int i=0;i<args.length;i+=2)
	  	  {	String str = args[i];
	  	  	String val = args[i+1];
	  	  	if(str.equals("-p")) { pass = val; }
	  	  	else if(str.equals("-h")) { host = val; }
	  	  	else if(str.equals("-u")) { user = val; }
	  	  	else if(str.equals("-d")) { database = val; }
	  	  	else if(str.equals("-o")) { out = val; }
	  	  	else if(str.equals("-i")) { in = val; }
	  	  	else if(str.equals("-class")) { classFile = val; }
	  	  	else { trouble=true; 
	  	  		System.out.println(str + " "+ val+" not understood"); 
	  	  		}
	  	  }
	  	  if(trouble) 
	  	  	{ System.out.println("use: -h host -u user -p password -d database -o path -i path");
	  	  	}
	  	  else {
	  			 Cardmaker cm = new Cardmaker();
	  			 JFrame fr = new JFrame();
	  			 fr.add(cm);
	  			 fr.setSize(400,800);
	  			 fr.setVisible(true);
	  			 cm.setVisible(true);
	  			 new Thread(cm).start();
	  			 try {
	  				 cm.makeCards(host,database,user,pass,in,out,classFile);
	  				 G.print(""+Line.lines.size()+" lines, "+Station.stations.size()+" stations, "+Stop.stops.size()
	  						 	+" stops = "+Station.nStops()+" on "+Station.nCards()+" cards");
	  			 }
	  			 catch(IOException e)
	  			 {
	  				 G.print("IO exception "+e);
	  			 }
	  			 catch(SQLException e)
	  			 {
	  				 G.print("Exception "+e.toString());
	  			 }
	  		  }
	  	  }
	@Override
	public void setLocalBounds(int l, int t, int w, int h) {
		fullRect = new Rectangle(l,t,w,h);
		repaint();
	}
	@Override
	public void StartDragging(HitPoint hp) {
		
	}
	@Override
	public void StopDragging(HitPoint hp) {
		
	}
	@Override
	public HitPoint MouseMotion(int eventx, int eventy, MouseState upcode) {
		return null;
	}
	public void run() {
		while(!exit)
		{
			ViewerRun(2000);
		}
	}
	@Override
	public void Pinch(int x, int y, double amount,double twist) {
		
	}
	@Override
	public void drawCanvasSprites(Graphics gc, HitPoint pt) {
		
	}
	  	  

}