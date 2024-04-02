package lib;

import com.codename1.ui.geom.Rectangle;

/**
 * this is a utility pop-up menu that has exit and other general options
 */
@SuppressWarnings("serial")
public class GearMenu extends Rectangle {

	private exCanvas parent = null;
	public GearMenu(exCanvas p) { parent = p; parent.addRect("gear"); }	
	PopupManager gearMenu = new PopupManager();
	
	enum GearId implements CellId
	{	Exit("Exit"),
		Feedback("Send Feedback"),
		DrawersOff("Player Drawers OFF"),
		DrawersOn("Player Drawers ON"),
		GearMenu("Options"),
		;
		
		String message;
		GearId(String m) { message = m; }


	}
	
	public void draw(Graphics gc,HitPoint hp)
	{
		StockArt.Gear.drawChip(gc, parent, this, hp,GearId.GearMenu,GearId.GearMenu.message); 
	}
	
	public void doGearMenu(int x,int y)
	{
		gearMenu.newPopupMenu(parent,parent);
		InternationalStrings s = G.getTranslations();
		gearMenu.addMenuItem(s.get(GearId.Exit.message),GearId.Exit);
		gearMenu.addMenuItem(s.get(GearId.Feedback.message),GearId.Feedback);
		if(G.isRealLastGameBoard())
		{
			gearMenu.addMenuItem(s.get(GearId.DrawersOff.message),GearId.DrawersOff);
			gearMenu.addMenuItem(s.get(GearId.DrawersOn.message),GearId.DrawersOn);
		}
		gearMenu.show(x,y);
	}
	
	public boolean handleDeferredEvent(Object target,String command)
	{
		if(gearMenu.selectMenuTarget(target))
		{
			GearId me = (GearId)gearMenu.rawValue;
			if(me!=null)
			{
				switch(me)
				{
				default: G.Error("Hit unexpected gear item %s",me);
					break;
				case Feedback:
				  	G.getFeedback();
				  	break;
				case DrawersOff:
					G.setDrawers(false);
					break;
				case DrawersOn:
					G.setDrawers(true);
					break;
				
				case Exit:	
						G.hardExit();
						break;
				}
				return true;
			}
			
		}
		return false;
	}
	
	static public void putStrings()
	{	for(GearId e : GearId.values()) { InternationalStrings.put(e.message); }
	}

	public boolean StopDragging(HitPoint hp) {
		if(hp.hitCode==GearId.GearMenu)
		{
			doGearMenu(G.Left(hp),G.Top(hp));
			return true;
		}
		return false;
	}	

}
