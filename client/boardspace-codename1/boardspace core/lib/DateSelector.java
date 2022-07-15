package lib;

import com.codename1.ui.geom.Rectangle;

import bridge.ActionEvent;
import bridge.ActionListener;
import bridge.Color;
import bridge.FontMetrics;

@SuppressWarnings("serial")
public class DateSelector extends Rectangle implements ActionListener
{	public enum DateCode implements CellId { Year,Month,Day;
		public String shortName() { return name(); }
	}
	int minYear = 0;
	int maxYear = 0;
	public boolean changed = false;
	@SuppressWarnings("deprecation")
	// day of the month
	public int getDate() { return date.getDate(); }
	// universal date/time 
	public long getTime() { return date.getTime(); }
	@SuppressWarnings("deprecation")
	public int minYear() 
	{	if(minYear==0) 
		{ minYear = 1900+date.getYear(); 
		}
		return minYear;
	}
	@SuppressWarnings("deprecation")
	public int maxYear()
	{ 	if(maxYear==0) { maxYear = 1900+date.getYear()+1; }
		return maxYear;
	}
	
	PopupManager menu = new PopupManager();
	DateCode selection = null;
	
	String prefix = "";
	BSDate date = new BSDate();
	Rectangle yearRect = new Rectangle();
	Rectangle monthRect = new Rectangle();;
	Rectangle dayRect = new Rectangle();;
	MenuParentInterface parent;
	public DateSelector(MenuParentInterface p,String pre,BSDate d)
	{
		super();
		parent = p;
		date = d;
		prefix = pre;
	}
	public void draw(Graphics gc,HitPoint hit)
	{
		String form = date.DateString().substring(0,10);
		String leader = prefix+" ";
		String message = leader+form;
		GC.Text(gc,true,this,Color.black,null,message);
		if(gc!=null)
			{ 	FontMetrics fm = gc.getFontMetrics(); 
				int plen = leader.length();
				Rectangle full = GC.getStringBounds(gc,fm,message);
				int left = G.Left(this)+(G.Width(this)-G.Width(full))/2;
				int top = G.Top(this)+(G.Height(this)-fm.getHeight())/2;
				yearRect =  GC.getStringBounds(gc,fm,message,plen,plen+4);
				G.translate(yearRect,left,top);
				
				monthRect = GC.getStringBounds(gc,fm,message,plen+4,plen+8);
				G.translate(monthRect,left,top);
				
				dayRect = GC.getStringBounds(gc,fm,message,plen+8,plen+10);
				G.SetWidth(dayRect,G.Width(dayRect)*2);
				G.translate(dayRect,left,top);
			}
		if(G.pointInRect(hit,yearRect)) 
		{	hit.hitCode = DateCode.Year;
			hit.spriteColor = Color.red;
			hit.spriteRect =yearRect;
		}
		if(G.pointInRect(hit,monthRect)) 
		{	hit.hitCode = DateCode.Month;
			hit.spriteColor = Color.red;
			hit.spriteRect =monthRect;
		}
		if(G.pointInRect(hit,dayRect)) 
		{	hit.hitCode = DateCode.Day;
			hit.spriteColor = Color.red;
			hit.spriteRect =dayRect;
		}
	}
	
	public void StopDragging(HitPoint hp)
	{	CellId hit = hp.hitCode;
		if(hit instanceof DateCode)
		{
		selection = (DateCode)hit;
		menu.newPopupMenu(parent,this);
		switch (selection)
		{
		case Year:
			for(int y = minYear(),lim=maxYear(); y<=lim;y++)
			{
				menu.addMenuItem(""+y,y);
			}
			break;
		case Month:
			for(int i=0; i<BSDate.months.length;i++)
			{
				menu.addMenuItem(BSDate.months[i],i);
			}
			break;
		case Day:
			for(int i=1;i<=31;i++)
			{
				menu.addMenuItem(" "+i+" ",i-1);
			}
			break;
		default: G.Error("Not expecting ",hit);
		}
		menu.show(G.Left(hp),G.Top(hp));

	}
	
	}
	@SuppressWarnings("deprecation")
	public void actionPerformed(ActionEvent e) {
		Object target = e.getSource();
		if(menu.selectMenuTarget(target))
		{	changed = true;
			int m = menu.value;
			switch(selection)
			{ case Year: date.setYear(m-1900); break; 
			case Month: date.setMonth(m); break;
			case Day: date.setDate(m); break;
			default: G.Error("Not expecting ",selection);
			}
		}
	}
}
