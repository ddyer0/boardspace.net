
package goban.shape.shape.gui;


import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Vector;

import goban.shape.beans.BoardActionEvent;
import goban.shape.beans.BoardActionListener;
import goban.shape.beans.DeltaOrnament;
import goban.shape.beans.LetterOrnament;
import goban.shape.beans.OrnamentProtocol;
import goban.shape.shape.*;
import goban.shape.shape.lisp.*;
import lib.Image;
import lib.G;
import lib.OStack;
class EventStack extends OStack<EventObject>
{
	public EventObject[] newComponentArray(int n) { return(new EventObject[n]); }
}
public class runner implements ActionListener,BoardActionListener, ItemListener, WindowListener,Runnable, Globals
{    // The fields used to hold the beans
	private class UndoMove
	{
		UndoMove prev = null;
		UndoMove next = null;
		LocationProvider moveat=null;
		boolean iswhite=false;
		Vector<Simple_Group> dead=null;
	}
	public int boardsize = 19;
	public Image Background_Image=null;
	private PrintStream console = null;
	private ShapePanel mainpanel;
	UndoMove history=null;
	boolean running = true;
	EventStack eventqueue = new EventStack();
	Object loaded_library=null;
	boolean needlibrary=true;
	ShapeLibrary library=null;
	URL liburl=null;
	URL sample_liburl=null;
	String libstring = null;
	String sample_libstring=null;
	boolean lispformat = true;
	X_Position test_x_position=null;		//the position (left center right) of the shape
	Y_Position test_y_position=null;		//the position (top center bottom) of the shape
	int test_x_org=0;										//the x location of the test shape on the board
	int test_y_org=0;										//the y location of the test shape on the board
	ShapeProtocol found_shape = null;		//the originally found shape
	ShapeProtocol test_shape = null;		//the shape being tested
	boolean test_shape_is_connected=false;
	ResultProtocol blackresults=null;		//multiresult for black
	ResultProtocol whiteresults =null;		//multiresult for white
	SingleResult whiteresult=null;			//the current result for white
	SingleResult blackresult=null;			//the current result for black
	LocationProvider black_move_location = null;		//where to move for white
	LocationProvider white_move_location = null;		//where to move for black
	
	
	static final String Modes[]={"Explore"," I play Black", " I play Red"};
	static final int mode_explore = 0;			//position of explore above
	static final int mode_play_black = 1;		//position of "I play black" above
	static final int mode_play_white = 2;		//position of "I play white" above
	
	/* constructor, from applet or main frame */
	public runner(ShapePanel panel,URL lib,URL sample_lib,boolean lisp_format, PrintStream cons,Image back)
	{ 	console = (cons==null)? System.out : cons;
		lispformat = lisp_format;
		mainpanel=(panel==null) ? new ShapePanel() : panel;
		Background_Image = back;
		liburl = lib;
		sample_liburl = sample_lib;
		initContents();	
	}
	/* constructor, from applet or main frame */
	public runner(ShapePanel panel,String lib,String sample_lib,boolean lisp_format, PrintStream cons,Image back)
	{ console = (cons==null)? System.out : cons;
		lispformat = lisp_format;
		mainpanel=(panel==null) ? new ShapePanel() : panel;
		libstring = lib;
		sample_libstring = sample_lib;
		Background_Image = back;
		initContents();	
	}
	
	public void SetMessage(String s)
	{
		mainpanel.message.setText(s);	
	}
	public void debug(String s)
	{
		if(G.debug()) { console.println(s); }	
	}
    // Initialize nested beans
    private void initContents()
    {			  
        // finish configuring the UI components
        mainpanel.positionboard.setboard_size_x(boardsize);
        mainpanel.positionboard.setboard_size_y(boardsize);
		mainpanel.positionboard.setzones_per_column(3);
		mainpanel.positionboard.setzones_per_row(3);
		mainpanel.positionboard.addBoardActionListener(this);
		mainpanel.positionboard.setSelected_At(new SimpleLocation(0,0));
		mainpanel.positionboard.Background_Image = Background_Image;
		
        mainpanel.shapeboard.setboard_size_x(boardsize);
        mainpanel.shapeboard.setboard_size_y(boardsize);
        mainpanel.shapeboard.SeePartBoard(6,6,12,12);
		mainpanel.shapeboard.addBoardActionListener(this);
		mainpanel.shapeboard.Background_Image = Background_Image;
		
        mainpanel.bigboard.setboard_size_x(boardsize);
        mainpanel.bigboard.setboard_size_y(boardsize);
		mainpanel.bigboard.addBoardActionListener(this);
		mainpanel.bigboard.Background_Image = Background_Image;
		
		for(int i=0;i<Modes.length;i++) { mainpanel.majormode.add(Modes[i]); }
		SetMajorMode(mode_explore);
		mainpanel.majormode.addItemListener(this);
		
		mainpanel.ToggleBlack.addItemListener(this);
		mainpanel.ToggleWhite.addItemListener(this);
		mainpanel.ChangeLibrary.addItemListener(this);
		mainpanel.ShowResults.addItemListener(this);
		mainpanel.ShowMoves.addItemListener(this);
		mainpanel.ShowInfo.addItemListener(this);
		
		mainpanel.BackButton.addActionListener(this);
		mainpanel.ForeButton.addActionListener(this);
		mainpanel.make_white_move_button.addActionListener(this);
		mainpanel.make_black_move_button.addActionListener(this);
	}
	
	/** receive item events */
	public void itemStateChanged(ItemEvent what)
	{ debug("Item " + what);
		QueueThisEvent(what);
	}
	/** receive incoming actions from our windows */
	public void actionPerformed(ActionEvent what)
	{
		debug("Action " + what);
		QueueThisEvent(what);
	}
	/** rexceive click events from our boards */
	public void boardActionPerformed(BoardActionEvent what)
	{
		debug("BoardAction " + what);
		QueueThisEvent(what);
		
	}
	/** save actions for the listener process, and make sure it's awake */
	synchronized void QueueThisEvent(EventObject t)
	{
		eventqueue.push(t);			
		notify();
	}
	
	/* window events, for when we are running as an application */
	public void windowOpened(WindowEvent e) 
	{//console.println("window event " + e);
	};
	public void windowClosing(WindowEvent e) 
	{	running=false;
		e.getWindow().dispose();	  
	};
	public void windowClosed(WindowEvent e) 
	{//console.println("window event " + e);
	};
	public void windowIconified(WindowEvent e)  
	{//console.println("window event " + e);
	};
	public void windowDeiconified(WindowEvent e) 
	{//console.println("window event " + e);
	};
	public void windowDeactivated(WindowEvent e) 
	{//console.println("window event " + e);
	};
	public void windowActivated(WindowEvent e) 
	{//console.println("window event " + e);
	};
	/** retrieve an event.  This occurs in our listener process which receives
	and processes all events 
	*/
	synchronized EventObject GetNextEvent()
	{
		try{
			while(eventqueue.size()==0 && running) { wait(); }
			if(eventqueue.size()!=0) 
			{ Object ev = eventqueue.pop();
			  return((EventObject)(ev)); 
			}	
		} catch (InterruptedException er) {};
		return(null);		
	}
	
	/** load the "lisp format" library.  This includes some netscape-only 
	kludgery */	
	void setLispLibrary(URL libraryname)
	{
		library = reader.Read_ShapeLibrary(libraryname,console);
		debug("lib is " + library);
	}
	void setLispLibrary(String outname,String libraryname)
	{
		library = reader.Read_ShapeLibrary(outname,libraryname,console);
		debug("lib is " + library);
	}
	
	void setJavaLibrary(URL libraryname)
	{
		library = new ShapeLibrary(libraryname);
		debug("lib is " + library);
		
	}	
	void setJavaLibrary(String libraryname)
	{
		library = new ShapeLibrary(libraryname);
		debug("lib is " + library);
	}	

	/** in response to the "find" button, place the new shape on the board,
	surround it with appropriate stones, and initialize the results display
	P is a board quadrant with x,y in 0-2
	*/
	void Place_At_Position(LocationProvider p,ShapeProtocol s)
	{
		int w = s.Width();
		int h = s.Height();
		int xmin=0;
		int xmax=0;
		int xorg=0;
		int yorg=0;
		int ymin=0;
		int ymax=0;
		
		/* figure out where to place the shape, based on the position board's
		selected position, which uses three zones left, center, right
		*/
		switch(p.getX())
		{
		case 0:
			xorg=xmin = 0;
			xmax = w;
			test_x_position = X_Position.Left;
			break;
		case 1:
			xmin =9-w/2-1;
			xorg=xmin+1;
			xmax =xorg+w;
			test_x_position = X_Position.Center;
			break;
		default: 
			xmax = 18;
			xmin = xmax-w;
			xorg = xmin+1;
			test_x_position = X_Position.Right;
		}
		
		switch(p.getY())
		{case 0:
			yorg=ymin = 0;
			ymax = h;
			test_y_position = Y_Position.Top;
			break;
		case 1: 
			ymin=9-h/2-1;
			yorg=ymin+1;
			ymax = yorg+h;
			test_y_position = Y_Position.Center;
			break;
		default:		
			ymax = 18;
			ymin = ymax-h;
			yorg = ymin+1;
			test_y_position=Y_Position.Bottom;
		}
		test_x_org=xorg;
		test_y_org=yorg;
		mainpanel.bigboard.Clear();
		for(int x=xmin; x<=xmax; x++)
		{	for(int y=ymin;y<=ymax;y++)
			{
				if(!s.ContainsPoint(x-xorg,y-yorg))
				{	mainpanel.bigboard.AddWhiteStone(new SimpleLocation(x,y));
				}					
			}
		}
		/* add a layyer of black stones all the way around */
		{int xl = Math.max(0,xmin-2);
			int xm = Math.min(18,xmax+2);
			int yl = Math.max(0,ymin-2);
			int ym = Math.min(18,ymax+2);
			for(int x=xl ; x<=xm; x++)
				for(int y=yl; y<=ym; y++)
				{ if(!s.ContainsPoint(x-xorg,y-yorg))
					{	SimpleLocation pt=new SimpleLocation(x,y);
						if(!mainpanel.bigboard.ContainsWhiteStone(pt))
						{mainpanel.bigboard.AddBlackStone(pt);
						}
			}}
				debug("from " + xl + " " + yl + "  to " +
					xm + " " + ym);
			mainpanel.bigboard.SeePartBoard(xl,yl,xm,ym);
		}
	}
	
	
	/** calculate the set of stones inside the current test group,
	return a bit mask indicating which ordinal positions are occupied
	*/
	int Intron_Mask()
	{ int mask=0;
		int maskbit=1;
		LocationProvider pts[] = test_shape.getPoints();
		int len = pts.length;
		for(int i=0;i<len;i++,maskbit=(maskbit<<1))
		{LocationProvider p=pts[i];
		 LocationProvider newpt = new SimpleLocation(p.getX()+test_x_org,p.getY()+test_y_org);
			if(mainpanel.bigboard.ContainsBlackStone(newpt))
			{ mask|=maskbit;
			}
		}
		return(mask);		
	}
	LocationProvider First_Test_Point()
	{	LocationProvider f = test_shape.getPoints()[0];
		return(new SimpleLocation(f.getX()+test_x_org, f.getY()+test_y_org));		
	}
	Simple_Group Largest_Group(Vector<Simple_Group> v)
	{long n=0;
		Simple_Group big=null;
		for(	Enumeration<Simple_Group> e=v.elements(); e.hasMoreElements();)
		{ Simple_Group g=e.nextElement();
			if(g.size()>n) { n=g.size(); big=g; }
		}
		return(big);
	}
	void ShapeClickAt(LocationProvider p)
	{	OrnamentProtocol orn = mainpanel.shapeboard.Remove_Ornament(p);
		if(orn==null)
		{ mainpanel.shapeboard.Add_Ornament(new DeltaOrnament(p,Color.red,true));
		}
	}
	void SetTestShape(ShapeProtocol s)
	{
		debug("found shape " + s);
		test_shape=s;
		if(test_shape==null) { SetMessage("No Data for this shape"); }  
		else { String msg = "the Shape is " + test_shape.getName();
			if(!test_shape_is_connected) 
			{ msg = msg + 
				"\n .. but the surrounding shape is disconnected"
					
					+ "\n   so the database is not directly applicable";
			}
			SetMessage(msg);	
		}}
	
	void FindNewShape()
	{if (found_shape!=null)
		{
			Simple_Group big_group = null;
			/* find the largest empty+black group in the old target area */
			{int width=found_shape.Width();
				int height=found_shape.Height();	  for(int x=0;x<width;x++)
				{for(int y=0;y<height;y++)
					{ LocationProvider pt = new SimpleLocation(x+test_x_org,y+test_y_org);
						if((big_group==null) || (big_group.containsPoint(pt)==null))
						{	
							Simple_Group g = new Simple_Group(BlackColor|EmptyColor,mainpanel.bigboard,pt);
							if((big_group==null)
								/* || ((g!=null) && g.Size()>big_group.Size())*/ )
							{big_group = g;
							}else
							{
								big_group.Union(g);
							}
						}}}}
			if(big_group!=null)
			{ 
				ShapeProtocol s = library.Find_Shape(big_group);
				if(s!=null) 
				{	LocationProvider mp = zhash.vectormin(big_group);
					int height = zhash.vectorheight(big_group);
					int width = zhash.vectorwidth(big_group);
					
					test_x_org = mp.getX(); 
					if(test_x_org==0) { test_x_position = X_Position.Left; }
					else if(test_x_org+width == boardsize) { test_x_position=X_Position.Right; }
					else {test_x_position = X_Position.Center; }
					
					test_y_org=mp.getY();
					if(test_y_org==0) { test_y_position = Y_Position.Top; }
					else  if(test_y_org+height == boardsize) { test_y_position=Y_Position.Bottom; }
					else {test_y_position = Y_Position.Center; }
				}
				SetTestShape(s);
			}
		}
	}
	/** find the largest white group surrounding the test shape.  */
	Simple_Group MainGroup(Simple_Group gr)
	{	Simple_Group maingroup = null;
		if(test_shape!=null)
		{ if(gr==null) 
			{ gr = new Simple_Group(BlackColor|EmptyColor, mainpanel.bigboard, First_Test_Point());
			}
			if(gr!=null)
			{
				Vector<Simple_Group> v = gr.Find_Adjacent_Groups(WhiteColor);	
				if(v!=null)
				{
					for(Enumeration<Simple_Group> e = v.elements(); e.hasMoreElements(); )
					{
						Simple_Group b = e.nextElement();
						if((maingroup==null) || (maingroup.size()<b.size()))
						{ maingroup = b; 
						}
					}	
				}
			}}
		return(maingroup);
	}
	LocationProvider FindOutsideLiberty()
	{
		Simple_Group gr = new Simple_Group(BlackColor|EmptyColor, mainpanel.bigboard, First_Test_Point());
		Simple_Group main = MainGroup(gr);
		if(main!=null)
		{Simple_Group libs = main.Find_Liberties();
		 if(libs!=null)
			{	for(int lim = libs.size()-1; lim>=0; lim--)
				{
					LocationProvider libpt = libs.elementAt(lim);
					if(gr.containsPoint(libpt)==null) 
					{/* if the inside group doesn't contain it, it must be an outside liberty */
						return(libpt); 
					}
				}
			}
		}
		return(null);
	}
	void RemoveGroup(Simple_Group gr)
	{ gr.Remove();
	}
	boolean RemoveDead(LocationProvider p,UndoMove his)
	{	boolean some_removed=false;
		int color = mainpanel.bigboard.ContainsWhiteStone(p)
			? WhiteColor
			: (mainpanel.bigboard.ContainsBlackStone(p) 
			? BlackColor : 0);
		if(color!=0)
		{Simple_Group group = new Simple_Group(color,mainpanel.bigboard,p);
			Vector<Simple_Group> enemies = group.Find_Adjacent_Groups(color==WhiteColor ? BlackColor : WhiteColor);
			if(enemies!=null)
			{ for(Enumeration<Simple_Group>e=enemies.elements(); e.hasMoreElements();) 
				{Simple_Group egr = e.nextElement();
					int libs = egr.N_Liberties();
					if(libs==0) 
		 	   	{ if(his.dead==null) { his.dead = new Vector<Simple_Group>(); }
						his.dead.addElement(egr);
						RemoveGroup(egr); some_removed=true;}
				}
			}
		}
		return(some_removed);
	}
	void ClickAt(LocationProvider p,boolean togglewhite)
	{
		if(p!=null)
		{ UndoMove his= new UndoMove();
			his.prev = history;
			his.moveat = p;
			his.iswhite = togglewhite;
			
			if(togglewhite)
			{ if(!mainpanel.bigboard.ContainsBlackStone(p))
				{ if(!mainpanel.bigboard.RemoveWhiteStone(p))
					{mainpanel.bigboard.AddWhiteStone(p);
						RemoveDead(p,his);
						FindNewShape();	//track the shape, watch for dropped shapes
					}else
					{ FindNewShape();
					}
				}
			}
			else if(!mainpanel.bigboard.ContainsWhiteStone(p))
			{
				if(!mainpanel.bigboard.RemoveBlackStone(p))
				{mainpanel.bigboard.AddBlackStone(p);
					if(RemoveDead(p,his)) 
					{ FindNewShape(); 
					}
				}
			}
			if(history!=null) { history.next = his; }
			history = his;
		}
		
		
		if(test_shape!=null)
		{int mask=Intron_Mask();
			LocationProvider first_point = First_Test_Point();
			Simple_Group test_group = new Simple_Group(BlackColor|EmptyColor,mainpanel.bigboard,first_point);
			Vector<Simple_Group> v = test_group.Find_Adjacent_Groups(WhiteColor);
			if(v!=null)
			{
				Simple_Group big_group = Largest_Group(v);
				int big_group_liberties = big_group.N_Liberties();
				int n_groups = v.size();
				
				whiteresults=test_shape.Fate_Of_Shape(Move_Order.First,test_x_position,test_y_position,mask);
				blackresults=test_shape.Fate_Of_Shape(Move_Order.Second,test_x_position,test_y_position,mask);
				
				whiteresult=test_shape.Exact_Fate_Of_Shape(Move_Order.First,test_x_position,test_y_position,
					mask,big_group_liberties);
				blackresult=test_shape.Exact_Fate_Of_Shape(Move_Order.Second,test_x_position,test_y_position,mask,
					big_group_liberties);
				debug("Mask is " + mask);
				debug("Big group is " + big_group);
				debug("N groups is " + n_groups );
				debug("White First: "+ whiteresult);
				debug("Black First: "+ blackresult);
				if(blackresult!=null) 
				{test_shape_is_connected = (n_groups == blackresult.N_Adjacent_Groups());
				}
				SetTestShape(test_shape);
			}}
		
		SetResults();
	}
	void UndoOneMove()
	{
		if(history.moveat!=null)
		{ UndoMove prev = history.prev;
			UndoMove current = history;
			if(current.dead!=null) 
			{/* restore the dead groups to the board */
				for(Enumeration<Simple_Group> e = current.dead.elements(); e.hasMoreElements(); )
				{ Simple_Group g = e.nextElement();
					g.Restore();
				}
				FindNewShape();			
			}
			history=null;
			ClickAt(current.moveat,current.iswhite);	//toggle the stone			
			history = prev;
		}
		else
		{SetMessage("this is the beginning"); }	
	}
	void RedoOneMove()
	{
		if(history.next!=null)
		{ UndoMove next = history.next;
			history=null;
			ClickAt(next.moveat,next.iswhite);
			history = next;
		}	else
		{SetMessage("this is the end, my friend, my only friend, the end....");
		}
	}
	/** add a letter ornament at the specified point.  If the point is already 
	occupied by a letter, return the same letter. If a new letter is allocated,
	use the next available, assuming that letters are used in sequence a-z
	*/
	String AddLetterOrnament(LocationProvider p)
	{	int i=0;
		String letterstring = null;
		Vector<OrnamentProtocol> ornaments = mainpanel.bigboard.Ornaments;
		
		for(Enumeration<OrnamentProtocol> e=ornaments.elements(); e.hasMoreElements();)
		{
			OrnamentProtocol orn = e.nextElement();
			if( (orn instanceof LetterOrnament)
				&& p.equals(orn.Location()) )
			{ letterstring = ((LetterOrnament)orn).text; 
			}
			i++;
		}
		/* not found, so add a new one */
		if(letterstring==null)
		{ letterstring = (""+((char)((int)'A'+i)));
			mainpanel.bigboard.Add_Ornament(new LetterOrnament(p,letterstring)); }
		return("at " + letterstring);
	}
	/** return the correct position to play for result r, or NULL if tenuki
	is the appropriate move.  If the move is a normal "inside" move, its derived
	from the test shape.  If it's an "outside" move, find the main white group
	and select one of its liberties that is not part of the test shape. */
	LocationProvider Position_To_Play(SingleResult r,SingleResult other)
	{
		LocationProvider pt = test_shape.Position_To_Play(r,other);
		if(pt==null)
		{int ord = r.Ordinal_Place_to_Play();
			if(ord==ordinal_move_outside /* code for play outside liberty */)
			{
				pt = FindOutsideLiberty();
			}
		}
		else { pt = new SimpleLocation(test_x_org+pt.getX(),test_y_org+pt.getY()); }
		return(pt);
	}
	String SetMarker(SingleResult r,LocationProvider pt)
	{	if(r==null){ return(""); }
		else 
		{ 
			if(pt!=null) { return(AddLetterOrnament(pt)); }
			else { int ord = r.Ordinal_Place_to_Play();
				if(ord==ordinal_move_pass) { return("tenuki"); }
				else if(ord==ordinal_move_outside) { return("outside"); }
				else {/* we get here when metaanalysis has shown that no move
					should be made, even though the database contains a move */
					return("no move needed"); }
			}
		}
		
	}
	boolean SameResult(SingleResult r,SingleResult o)
	{ Fate rf=r.getFate();
		return(//(rf==Fate.Dead_With_Eye)||(rf==Fate.Dead)&&
			(rf==o.getFate()));
	}
	String SetBlackMarker(SingleResult br,SingleResult wr,LocationProvider pt)
	{	String base = SetMarker(br,pt);
		if((pt!=null) && SameResult(br,wr))
		{//NamedObject fate = br.Fate();
		base = "Ko threat " + base; 
		}
		return(base);			
	}
	String SetWhiteMarker(SingleResult wr,SingleResult br,LocationProvider pt)
	{	String base = SetMarker(wr,pt);
		if((pt!=null) && SameResult(wr,br))
		{//NamedObject fate = wr.Fate();
			/* meta analysis based on both black and white results.  If the outcome
			is alive no matter what, white can tenuki.  */
			base = "Reduce Ko " + base; 
		}
		return(base);			
	}
	String DifferentResults(ResultProtocol multi,SingleResult single)
	{
		SingleResult more =multi.Fate_for_N_Liberties(99);
		SingleResult less = multi.Fate_for_N_Liberties(0);
		if((more!=single)&&(less!=single)) { return("try different liberties");}
		if(more!=single) { return("try more liberties"); }
		if(less!=single) { return("try fewer liberties"); }
		return("");
	}
	void SetResults()
	{mainpanel.bigboard.Clear_Ornaments();
		String whiteresultstring="";
		String blackresultstring="";
		String blackmovestring = "";
		String whitemovestring = "";
		String blackinfostring = "";
		String whiteinfostring = "";
		
		if (test_shape==null)
		{
			black_move_location = null;
			white_move_location = null;
		}else
		{	boolean showresults = mainpanel.ShowResults.getState();
			boolean showmoves = mainpanel.ShowMoves.getState();
			boolean showInfo = mainpanel.ShowInfo.getState();
			
			if(blackresult!=null)
			{ blackresultstring = (showresults
				? blackresult.getFate().toString()
					: "");
				black_move_location = Position_To_Play(blackresult,whiteresult);
				blackmovestring = (showmoves? SetBlackMarker(blackresult,whiteresult,black_move_location) : "");
				if(showInfo) { blackinfostring = blackresult.Aux_String(); 
					if(blackresult!=blackresults)
					{blackinfostring = DifferentResults(blackresults,blackresult);
					}
					
				}
			}
			if(whiteresult!=null)
			{whiteresultstring = ( showresults
				? whiteresult.getFate().toString()
					: "");
				white_move_location = Position_To_Play(whiteresult,blackresult);
				whitemovestring = (showmoves? SetWhiteMarker(whiteresult,blackresult,white_move_location) : "");  
				if(showInfo) { whiteinfostring = whiteresult.Aux_String(); }
				if(whiteresult!=whiteresults)
				{whiteinfostring = DifferentResults(whiteresults,whiteresult);
				}
			}
		}
		mainpanel.BlackResult.setText(blackresultstring);
		mainpanel.WhiteResult.setText(whiteresultstring);
		mainpanel.BlackMove.setText(blackmovestring);
		mainpanel.WhiteMove.setText(whitemovestring);
		mainpanel.BlackAuxText.setText(blackinfostring); 
		mainpanel.WhiteAuxText.setText(whiteinfostring);
	}
	
	int GetMajorMode() 
	{ return(mainpanel.majormode.getSelectedIndex());
	}
	
	void SetMajorMode(int index)
	{ boolean playmode = (index!=mode_explore);
		if(index!=GetMajorMode()) { mainpanel.majormode.select(index); }
		mainpanel.ToggleBlack.setVisible(!playmode);
		mainpanel.ToggleWhite.setVisible(!playmode);
		if(playmode) 
		{ /* set them both, since if they're invisble the events won't fire to make them
			toggle one another */
			mainpanel.ToggleBlack.setState(index==mode_play_black); 
			mainpanel.ToggleWhite.setState(index!=mode_play_black);
		}		
	}
	/** make a move and a responding move is required by the mode */
	void MoveAt(LocationProvider p,boolean iswhite)
	{
		ClickAt(p,iswhite);	
		switch(GetMajorMode())
		{
		case mode_play_white: if(iswhite) { ClickAt(black_move_location,false);};
			break;
		case mode_play_black: if(!iswhite) { ClickAt(white_move_location,true);};
			break;
		default:	;
		}
	}
	public void mainevent()
	{
		EventObject e = GetNextEvent();
		debug("Handle event " + e);
		Object source = e.getSource();
		
		if(source==(Object)mainpanel.findbutton)
		{ Vector<OrnamentProtocol> v = mainpanel.shapeboard.Ornaments;
	  	  LocationProvider vp[] = new SimpleLocation[v.size()];
	  	  for(int i=v.size()-1; i>=0;i--)
	  	    {vp[i]=v.elementAt(i).Location();
			}
	  	  ShapeProtocol s = library.Find_Shape(vp);
	  	  found_shape = s;
	  	  history = new UndoMove();
	  	  SetTestShape(s);
	  	  if(s!=null)
	  	  {LocationProvider p = mainpanel.positionboard.getSelected_At();
				Place_At_Position(p,s);
				ClickAt(null,false);
	  	  }else
	  	  {mainpanel.bigboard.Clear();
	  	  }
		}
		else if(source==mainpanel.positionboard) 
		{ mainpanel.positionboard.setSelected_At(((BoardActionEvent)e).Mouse_At);
		}
		else if(source==mainpanel.shapeboard)
		{
			ShapeClickAt(((BoardActionEvent)e).Mouse_At);	
		}
		else if(source==mainpanel.bigboard)
		{boolean togglewhite = mainpanel.ToggleWhite.getState();
			MoveAt(((BoardActionEvent)e).Mouse_At,togglewhite);	
		}
		else if(source==mainpanel.majormode)
		{ SetMajorMode(GetMajorMode());
		}
		else if(source==mainpanel.ChangeLibrary)
		{ library=null; needlibrary=true;test_shape=null;
		}
		else if(source==mainpanel.ToggleWhite)
		{ mainpanel.ToggleBlack.setState(!mainpanel.ToggleWhite.getState());
	    }
		else if(source==mainpanel.ToggleBlack)
		{ mainpanel.ToggleWhite.setState(!mainpanel.ToggleBlack.getState());
	    }
	    else if((source==mainpanel.ShowMoves)
			||(source==mainpanel.ShowResults)
				||(source==mainpanel.ShowInfo))
	    { SetResults();
	    }
	    else if(source==mainpanel.BackButton)
	    {debug("Backward");
			UndoOneMove();
		}
	    else if(source==mainpanel.ForeButton)
	    {debug("Foreward");
			RedoOneMove();
	    }
	    else if(source==mainpanel.make_white_move_button)
	    { MoveAt(white_move_location,true);
	    }
	    else if(source==mainpanel.make_black_move_button)
	    { MoveAt(black_move_location,false);
	    }
		else
		{
			console.println("Strange event " + e);	
		}	
	}
	
	boolean load_url_library(boolean full)
	{	URL pref = full 
		? (liburl==null? sample_liburl :liburl) 
			: sample_liburl==null ? liburl : sample_liburl;
		if(pref!=null)
		{
			SetMessage("Reading database from " + pref);
			
			if(lispformat)
			{setLispLibrary(pref);
			}else
			{setJavaLibrary(pref);
			}}
		return(pref!=null);
	}
	boolean load_file_library(String outname,boolean full)
	{
		String pref = full 
			? (libstring==null? sample_libstring :libstring) 
			: sample_libstring==null ? libstring : sample_libstring;
		if(pref!=null)
		{
			SetMessage("Reading database from " + pref);
			if(lispformat)
			{setLispLibrary(outname,pref);
			}else
			{setJavaLibrary(pref);
			}}
		return(pref!=null);
	}
	
	void loadlibrary(String outname,boolean full)
	{
		
		try
		{if(!load_url_library(full) ) { load_file_library(outname,full);}
			SetMessage("Ready.  Set up a pattern and click on FIND");
			debug("Single Hits " + SingleResult.Hits + " Misses " + SingleResult.Misses);
			debug("Multi Hits " + MultiResult.Hits + " Misses " + MultiResult.Misses);
		} finally {
			mainpanel.shapeboard.Clear();		//and clear the progress display	
			FindNewShape();
		}}
	
	/* for runnable */
	public void run()
	{	mainpanel.findbutton.addActionListener(this);
		mainpanel.findbutton.setEnabled(true);
		if(running) { debug("Running"); }
		{int errs=0;
			while(running && (errs<3))
			{ try {
					errs++;
					if(needlibrary) 
						{ needlibrary=false; loadlibrary(null,mainpanel.ChangeLibrary.getState()); } 
					mainevent();
					errs=0;
				} 
				catch (Throwable e)
				{ console.println("Error in main loop " + e);
				  G.printStackTrace(e,console);
				  running=false;
				}
			
			}}
		running=false;
	}
}
