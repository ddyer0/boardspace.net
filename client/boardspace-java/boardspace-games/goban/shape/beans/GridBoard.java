package goban.shape.beans;

import bridge.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import lib.Graphics;
import lib.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import goban.shape.shape.LocationProvider;
import goban.shape.shape.SimpleLocation;
import lib.G;
import lib.GC;

public class GridBoard extends Canvas
                            implements Serializable,
                            MouseListener, MouseMotionListener 
{/* bean properties */

/**
	 * 
	 */
	static final long serialVersionUID = 1L;
/** the selected zone.  Zones are numbered left-right top-bottom, as though the entire board
    was displayed.  Zones are usually the same size as squares.
 */
 private LocationProvider Selected_At=null;
 public LocationProvider Mouse_At=null;
 public void setSelected_At(LocationProvider p)
 {
 	Selected_At = p;
 	repaint();
 }
 public LocationProvider getSelected_At() { return(Selected_At); }
 
 public Vector<LocationProvider> BlackStones = new Vector<LocationProvider>();
 public Vector<LocationProvider> WhiteStones = new Vector<LocationProvider>();
 public Vector<OrnamentProtocol> Ornaments = new Vector<OrnamentProtocol>();

 private Image active_image=null;
 private Dimension active_dim=null;
 
/* the nuber of horizontal zones */
 private int zones_per_row = 19;
 public int getzones_per_row() { return(zones_per_row); };
 public void setzones_per_row(int val) { zones_per_row = val; };
 
/** the number of vertical zones */
 private int zones_per_column = 19;
 public int getzones_per_column() { return(zones_per_column); };
 public void setzones_per_column(int val) { zones_per_column = val; };

 /** the number of horizontal rows to display */
 public int getVisible_Columns() 
 	{ return(grid_visible_columns=(display_x_max-display_x_min+1)); 
 	};

/** the number of vertical rows to display */
 public int getVisible_Rows()
 	 { return(grid_visible_rows=(display_y_max-display_y_min+1)); 
 	 };


/** the last column to be displayed */
 private int board_size_x = 19;
 public int getboard_size_x() { return(board_size_x); };
 public void setboard_size_x(int val) { board_size_x = val; };

/** the full board vertical size (in squares) */
 private int board_size_y = 19;
 public int getboard_size_y() { return(board_size_y); };
 public void setboard_size_y(int val) { board_size_y = val; };


/** the first column to be displayed */
 private int display_x_min = 0;
 public int getdisplay_x_min() { return(display_x_min); };
 public void setdisplay_x_min(int val) { display_x_min = val; };

/** the last column to be displayed */
 private int display_x_max = 18;
 public int getdisplay_x_max() { return(display_x_max); };
 public void setdisplay_x_max(int val) { display_x_max = val; };

/** the first row to be displayed */
 private int display_y_min = 0;
 public int getdisplay_y_min() { return(display_y_min); };
 public void setdisplay_y_min(int val) { display_y_min = val; };

/** the first row to be displayed */
 private int display_y_max = 18;
 public int getdisplay_y_max() { return(display_y_max); };
 public void setdisplay_y_max(int val) { display_y_max = val; };

/** the horizontal size of the whole board, including margins */
 private int size_x = 100;
 public int getsize_x() { return(size_x); };
 public void setsize_x(int val) { size_x = val; };

/** the vertical size of the whole board, including margins */
 private int size_y = 100;
 public int getsize_y() { return(size_y); };
 public void setsize_y(int val) { size_y = val; };


 private Color SelectedColor = new Color((float)0.6,(float)0.4,(float)0.4);
 public Color getSelectedColor() { return(SelectedColor); };
 public void setSelectedColor(Color val) { SelectedColor = val; };

 private Color MouseColor = new Color((float)0.6,(float)0.6,(float)0.6);
 public Color getMouseColor() { return(MouseColor); };
 public void setMouseColor(Color val) { MouseColor = val; };

 private Color ForegroundColor = Color.black;
 public Color getForegroundColor() { return(ForegroundColor); };
 public void setForegroundColor(Color val) { ForegroundColor = val; };

 private Color BackgroundColor = new Color((float)0.4,(float)0.2,(float)0.2);
 public Color getBackgroundColor() { return(BackgroundColor); };
 public void setBackgroundColor(Color val) { BackgroundColor = val; };
 
 public Image Background_Image=null;
 
 private String Background_Image_Name = null;
 public String getBackground_Image_Name() { return(Background_Image_Name); }
 public void setBackGround_Image_Name(String val) { Background_Image_Name = val; }
 
 // truely private data
  int grid_origin_x = 0;
  int grid_origin_y = 0;
  int grid_step_x = 1;
  int grid_step_y = 1;
  int grid_visible_rows = 1;
  int grid_visible_columns = 1;

 public void SeeWholeBoard()
 {	setdisplay_x_min(0);
 	  setdisplay_y_min(0);
 	  setdisplay_x_max(getboard_size_x()-1);
 	  setdisplay_y_max(getboard_size_y()-1);
 		setzones_per_column(getVisible_Columns());
 		setzones_per_row(getVisible_Rows());
 }
  public void SeePartBoard(int xfrom,int yfrom, int xto,int yto)
 {	setdisplay_x_min(Math.max(0,xfrom));
 	  setdisplay_y_min(Math.max(0,yfrom));
 	  setdisplay_x_max(Math.min(xto,getboard_size_x()-1));
 	  setdisplay_y_max(Math.min(yto,getboard_size_y()-1));
 		setzones_per_column(getVisible_Columns());
 		setzones_per_row(getVisible_Rows());
 		repaint();
 }

 /* convert an x,y into a zone number, or -1. */
 private LocationProvider zone_for_xy(int x,int y)
 {int minx = grid_origin_x-grid_step_x/2;
  int miny = grid_origin_y-grid_step_y/2;
  int maxx = minx+(grid_visible_columns)*grid_step_x;
  int maxy = miny+(grid_visible_rows)*grid_step_y;
 
 	if(x<minx || x>=maxx || y<miny || y>=maxy) 
	 	{return(null); 
	 	}
 	else
	 {int zone_w = (maxx-minx)/zones_per_column;
	 	int zone_h = (maxy-miny)/zones_per_row;
	 	int vis_square_x = ((x-minx)/zone_w);
	 	int vis_square_y = ((y-miny)/zone_h);
		int square_x = vis_square_x + display_x_min;
		int square_y = vis_square_y + display_y_min;
	  return(new SimpleLocation(square_x,square_y));
	 }
 }
 void SetGridParameters()
 	{
	square_size_x();	//sets grid_step_x grid_size_x
  square_size_y();	//sets grid_step_y grid_size_y

  int line_height = grid_step_y*(grid_visible_rows-1);
  int line_width = grid_step_x*(grid_visible_columns-1);
  
  /* adjust the line origin and lengths if the board is partial instead of full */
   grid_origin_x = (size_x-line_width)/2;
   grid_origin_y = (size_y-line_height)/2;	
 
  if(display_x_min>0) { line_width+= grid_step_x/2; }
  if((display_x_max+1) < board_size_x) { line_width += grid_step_x/2; }
  if(display_y_min>0) { line_height+= grid_step_y/2; }
  if((display_y_max+1) < board_size_y) { line_height += grid_step_y/2; }
 	}
 	
 private void FinishSetup()
 	{
   setSize(size_x,size_y);
   SeeWholeBoard();	//make sure parameters start consistantly
   setBackground(BackgroundColor);
   addMouseListener(this);
	 addMouseMotionListener(this);
 	}	
 		
 			
 
//Constructor sets inherited properties
 public GridBoard(){
		FinishSetup();
	}
 public GridBoard(int size)
 	{ this.board_size_x = board_size_y= size;
 	  FinishSetup();
  }





	public void PaintWhiteStone(Graphics g,LocationProvider p)
	{
	 GC.setColor(g,Color.white);
	 PaintStone(g,p);
	}
 	public void PaintBlackStone(Graphics g,LocationProvider p)
	{
	 GC.setColor(g,Color.black);
	 PaintStone(g,p);
	}	
 public LocationProvider Square_Center(LocationProvider p)
 {		int x = p.getX()-getdisplay_x_min();
			int y = p.getY()-getdisplay_y_min();
	 	return(new SimpleLocation(x*grid_step_x+grid_origin_x, y*grid_step_y+grid_origin_y));
 }
 public int Square_Width()
 {	return(grid_step_x);
 }
 public int Square_Height()
 {	return(grid_step_y);
 }
 void PaintStone(Graphics g,LocationProvider p)
	{
			if(IsVisible(p)) 
    {
		int x = p.getX()-getdisplay_x_min();
			int y = p.getY()-getdisplay_y_min();
			GC.fillOval(g, x*grid_step_x+grid_origin_x-grid_step_x/2,
								  y*grid_step_y+grid_origin_y-grid_step_y/2,
								  grid_step_x,
								  grid_step_y);
		}	
}
	private LocationProvider ContainsPoint(Vector<LocationProvider> v,int x,int y)
	{	for( Enumeration<LocationProvider> e=v.elements(); e.hasMoreElements(); )
		{ LocationProvider el = e.nextElement();
		  if(el.equals(x,y)) return(el);
		 }
		 return(null);
	}
	private LocationProvider ContainsPoint(Vector<LocationProvider> v,LocationProvider p)
	{
		return(ContainsPoint(v,p.getX(),p.getY()));
	}
	public boolean isInside(LocationProvider p)
	{
	 return(isInside(p.getX(),p.getY()));
	}
	public boolean isInside(int x,int y)
	{
	 return(	(x>=0) && (x<board_size_x) && (y>=0) && (y<board_size_y));
	}
	public boolean isEmpty(LocationProvider p)
	{	return(isEmpty(p.getX(),p.getY()));
	}
	public boolean isEmpty(int x,int y)
	{
		return(isInside(x,y)
			  && !ContainsWhiteStone(x,y) && !ContainsBlackStone(x,y));			
	}

/** return true if the board contains a white stone at the indicated point */
	public boolean ContainsWhiteStone(LocationProvider p)
	{ return(ContainsWhiteStone(p.getX(),p.getY()));
	}
	public boolean ContainsWhiteStone(int x,int y)
	{return(ContainsPoint(WhiteStones,x,y)!=null);
	}


  /** return true if the board contains a black stone at the indicated point
   */
	public boolean ContainsBlackStone(LocationProvider p)
	{ return((ContainsPoint(BlackStones,p.getX(),p.getY())!=null));
	}

	  /** return true if the board contains a black stone at the indicated point
	   */
		public boolean ContainsBlackStone(int x,int y)
		{ return(ContainsPoint(BlackStones,x,y)!=null);
		}
  /** remove a white stone at the indicated point, return true if there
  was one there to be removed.
  */	
  public boolean RemoveWhiteStone(LocationProvider np)
  {	LocationProvider p=ContainsPoint(WhiteStones,np);
    if(p!=null) 
    	{ WhiteStones.removeElement(p); 
   		  repaint();
   			return(true); 
    	}
    else { return(false); }
  }

  /** remove a Black stone at the indicated point, return true if there
  was one there to be removed.
  */	
  public boolean RemoveBlackStone(LocationProvider np)
  {	LocationProvider p=ContainsPoint(BlackStones,np);
    if(p!=null) 
    	{ BlackStones.removeElement(p); 
        repaint();
    	  return(true); 
    	  }
    else { return(false); }
  }
  	
  /** add a black stone to the board.  If it is aready there, do nothing.
   Return true if a stone was added, false if the stone was already there
   */
  public boolean AddBlackStone(LocationProvider p)
  { if(!ContainsBlackStone(p)) 
  		{ BlackStones.addElement(p); 
   		  repaint();
 		  	return(true);
  		}
  		else { return(false);
  		}
  }
  /** add a white stone to the board.  If it is aready there, do nothing.
   Return true if a stone was added, false if the stone was already there
   */
  public boolean AddWhiteStone(LocationProvider p)
  { if(!ContainsWhiteStone(p)) 
  		{ WhiteStones.addElement(p); 
  		  repaint();
  			return(true);
  		}
  		else { return(false);
  		}
  }
  /** make the board empty */
  public void Clear()
  {	BlackStones.setSize(0);
    WhiteStones.setSize(0);
    Ornaments.setSize(0);
  	repaint();
  }
  public void Clear_Ornaments()
  {	int sz=Ornaments.size();
    Ornaments.setSize(0);
    if(sz>0) { repaint();}
  }
  public void Add_Ornament(OrnamentProtocol o)
  	{ Ornaments.addElement(o);
  	  repaint();
  	}
  public OrnamentProtocol OrnamentAt(LocationProvider p)
  {
  	for(Enumeration<OrnamentProtocol> e=Ornaments.elements(); e.hasMoreElements(); )
  		{ OrnamentProtocol orn = e.nextElement();
  		  if(orn.Location().equals(p)) {return(orn);}
  			}	
  	return(null);
  }
  public OrnamentProtocol Remove_Ornament(LocationProvider p)
  {
  	OrnamentProtocol orn = OrnamentAt(p);
  	if(orn!=null) { Ornaments.removeElement(orn); repaint();}
  	return(orn);	
  }
  /* return true if the indicated point is visible */
 	public boolean IsVisible(LocationProvider p)
  {
	  return(p.getX()>=getdisplay_x_min() 
					&& p.getX() <= getdisplay_x_max()
					&& p.getY() >= getdisplay_y_min()
					&& p.getY() <= getdisplay_y_max());
	}

 public void paint(Graphics realg)
 {	Dimension d = getSize();
 	if((active_image==null) || (!active_dim.equals(d)))
 		 { 
 		 	 active_dim=d;
 		   active_image = Image.createImage(G.Width(d),G.Height(d));
 		   if((Background_Image==null) && (Background_Image_Name!=null))
 		   	{ Background_Image = Image.createImage(getToolkit().getImage(Background_Image_Name));
 		   }
 		 }
 {Graphics g = active_image.getGraphics();
 Rectangle rb=getBounds();
 int height = size_y= G.Height(rb);
 int width = size_x = G.Height(rb);		//set the current w/h for other calculations

 /* fill background */
 GC.setColor(g,BackgroundColor);
 GC.fillRect(g,0,0,width,height);	//do this anyway, in case the image isn't loaded or found
 if(Background_Image!=null)
 	{ int background_width = Background_Image.getWidth();
  		  int background_height = Background_Image.getHeight();
  		  int size_y = board_size_y-1;
  		  int size_x = board_size_x-1;
  		  int xo = (display_x_min*background_width)/size_x;
  		  int yo = (display_y_min*background_height)/size_y;
  		  int xe = (display_x_max*background_width)/size_x;
  		  int ye = (display_y_max*background_height)/size_y;
  		 Background_Image.drawImage(g,0,0,width,height,xo,yo,xe,ye); 
  		  }
 
 paint_background_zones(g);
 paint_lines(g);
 paint_stones(g);
 paint_ornaments(g);
 }
active_image.drawImage(realg,0,0);
 }

 public void update(Graphics g)
 {	try {
 	paint(g);
 	}
 	catch (Throwable err)
 	{
	System.out.println("Exception in repaint "+err); 
 	}
 }
 public void paint_ornaments(Graphics g)
 {
 		for(Enumeration<OrnamentProtocol> e=Ornaments.elements(); e.hasMoreElements(); )
 		{
 			OrnamentProtocol orn = (OrnamentProtocol)e.nextElement();
 			orn.Draw(g,this);	
 		}
 }
 public void paint_stones(Graphics g)
 {
 		for(Enumeration<LocationProvider> e=WhiteStones.elements(); e.hasMoreElements(); )
		{
			PaintWhiteStone(g,e.nextElement())	;
		}
		for(Enumeration<LocationProvider> e=BlackStones.elements(); e.hasMoreElements(); )
		{
			PaintBlackStone(g,e.nextElement())	;
		}
	
 	
 } 	
/* we have separate x and y size calculations so int the future, the 
 squares can be slightly nonsquare */ 
public int square_size_x()
{
 int Visible_Rows = getVisible_Rows();
 int Visible_Columns = getVisible_Columns();
 int szx = size_x/(Visible_Columns+1);
 int szy = (int)(size_y/(Visible_Rows+1));
 return(grid_step_x = Math.min(szx,szy));
}
public int square_size_y()
{
	return(grid_step_y = square_size_x());
}
/* paint the lines */
private void paint_lines(Graphics g)
 {SetGridParameters();
  int line_initial_x = grid_origin_x;
  int line_initial_y = grid_origin_y;
  int line_height = grid_step_y*(grid_visible_rows-1);
  int line_width = grid_step_x*(grid_visible_columns-1);
  GC.setColor(g,ForegroundColor);

  if(display_x_min>0) { line_initial_x -= grid_step_x/2; line_width+=grid_step_x/2; }
  if((display_x_max+1)<board_size_x) { line_width+=grid_step_x/2; }
  if(display_y_min>0) { line_initial_y -= grid_step_y/2; line_height+=grid_step_y/2; }
  if((display_y_max+1)<board_size_y) { line_height+=grid_step_y/2; }
  
    
  for(int x=grid_origin_x, col=0; 
      col<grid_visible_columns;
      col++,x+=grid_step_x)
     { g.drawLine(x,line_initial_y,x,line_initial_y+line_height); 
     }
     
  for(int y=grid_origin_y, row=0; 
      row<grid_visible_rows;
       row++,y+=grid_step_y)
     { g.drawLine(line_initial_x,y,line_initial_x+line_width,y); 
 		 }
 } 
 /* paint the shaded background for the zone and mouse zone, if any */
 private void paint_background_zones(Graphics g)
 {
 	
 if(zones_per_row>0 && zones_per_column>0)
 {
  /* paint current zone */
 int x_zone_size =(grid_step_x*grid_visible_columns)/zones_per_column;
 int y_zone_size =(grid_step_y*grid_visible_rows)/zones_per_row;
 
 if(Selected_At!=null)
	 {int sel_x_zone = Selected_At.getX();
	 int sel_y_zone = Selected_At.getY();
	 int x_zone = sel_x_zone-display_x_min;
	 int y_zone = sel_y_zone-display_y_min;
	 GC.setColor(g,SelectedColor);
	 GC.fillRect(g,grid_origin_x+x_zone*x_zone_size-grid_step_x/2,
	 						grid_origin_y+y_zone*y_zone_size-grid_step_y/2,
	 									x_zone_size+1,
	 									y_zone_size+1);
	 }
 
 if(Mouse_At!=null && ((Selected_At==null) || !Mouse_At.equals(Selected_At)))
	 {
	 	int mouse_x_zone = Mouse_At.getX();
	 	int mouse_y_zone = Mouse_At.getY();
	 	int x_zone = mouse_x_zone - display_x_min;
	 	int y_zone = mouse_y_zone - display_y_min;
	 	GC.setColor(g,MouseColor);
	 	GC.fillRect(g,grid_origin_x+x_zone*x_zone_size-grid_step_x/2, 
	 						grid_origin_y+y_zone*y_zone_size-grid_step_y/2,
	 				     x_zone_size+1,
	 				     y_zone_size+1);
  	}	
  } /* finished painting zones */
 	
 }
 


 // Mouse listener methods.

    public void mouseClicked(MouseEvent evt) 
    {
    	if((Mouse_At!=null) && ((Selected_At==null)|| !Mouse_At.equals(Selected_At)))
    		{FireAction(Mouse_At);
      		repaint();
    	  }
    }

   public void mouseMoved(MouseEvent evt)
   	{
   	 int x = evt.getX();
   	 int y = evt.getY();
   	 LocationProvider new_zone = zone_for_xy(x,y);
   	 if((new_zone!=null) && ((Mouse_At==null) || !new_zone.equals(Mouse_At)))
   	 {
   	 		Mouse_At = new_zone;
   	 		repaint();
   	 }
    }
   public void mousePressed(MouseEvent evt) {
    }
    
    public void mouseReleased(MouseEvent evt) {
    }
    
    public void mouseEntered(MouseEvent evt) {
    }

    public void mouseExited(MouseEvent evt) {
    Mouse_At = null;
    repaint();
    }

    public void mouseDragged(MouseEvent evt) {
		}
	
	
   private Vector<BoardActionListener> pushListeners=new Vector<BoardActionListener>();    
   public synchronized void addBoardActionListener(BoardActionListener l) {
			pushListeners.addElement(l);
    }
    public synchronized void removeBoardActionListener(BoardActionListener l) {
			pushListeners.removeElement(l);
    }
    /** 
     * This method has the same effect as pressing the button.
     * 
     * @see #addActionListener
     */  
public void FireAction(LocationProvider at) {
		Vector<BoardActionListener> targets = new Vector<BoardActionListener>();
		synchronized (this) {
			for(int i=0,lim=pushListeners.size(); i<lim;i++)
			{	targets.addElement(pushListeners.elementAt(i));
			}
		}
		BoardActionEvent actionEvt = new BoardActionEvent(this, 0, null,at,at.getX(),at.getY());
		for (int i = 0; i < targets.size(); i++) {
		    BoardActionListener target = (BoardActionListener)targets.elementAt(i);
		    target.boardActionPerformed(actionEvt);
		}

  }
	
	}