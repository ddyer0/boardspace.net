package bridge;

public class PopupMenu extends Menu {
	private boolean showing = false;
	public PopupMenu() { super(); }
	public PopupMenu(String m) { super(m); }
	
	// this "light weight popup" stuff relates to using native menus
	// rather than java widgets.
	private static boolean defaultLightWeightPopup = true; 
	@SuppressWarnings("unused")
	private boolean lightWeightPopup = defaultLightWeightPopup;
	public void setLightWeightPopupEnabled(boolean b) { lightWeightPopup = b; }
	public static void setDefaultLightWeightPopupEnabled(boolean b) { defaultLightWeightPopup = b; }

	public void actionPerformed(ActionEvent e)
	{	super.actionPerformed(e);
		Object target = e.getSource();
		com.codename1.ui.Container on = getShowingOn();
		if((target==getMenu()) && (on!=null))
			{ getMenu().setVisible(false);
			  showing=false; 
			} 
	}
	public boolean isVisible() {
		// this gets called during initialization for undetermined reasons.
		return(showing); 
		}
	public boolean getTopLevel() { return(true); }

}
