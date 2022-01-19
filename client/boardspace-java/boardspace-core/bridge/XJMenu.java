package bridge;

import java.awt.event.MouseEvent;

import lib.G;

public class XJMenu extends JMenu {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public XJMenu(String title,boolean noAutoPop) { super(title); inhibitpopup = noAutoPop; } 
	public boolean inhibitpopup = false;
	protected void processMouseEvent(MouseEvent e) 
	{
		try {
			int id = e.getID();
			switch(id)
            {
            case MouseEvent.MOUSE_ENTERED:	
            	// inhibit mouse entered to avoid auto-selection moving
            	// between items on the jmenubar
            	if(inhibitpopup) { break; }
            default: super.processMouseEvent(e);
            }
        }
		catch (ArrayIndexOutOfBoundsException err) 
		{ G.print("error in java menu "+err);
		}
	}
	
}
